package com.vivid.service.impl;

import com.vivid.exception.ResourceNotFoundException;
import com.vivid.exception.InvalidRequestException;
import com.vivid.exception.InvalidStateTransitionException;
import com.vivid.model.dto.ArticleServiceDto;
import com.vivid.model.enums.ArticleStatus;
import com.vivid.model.payloads.requests.ArticleCreateRequest;
import com.vivid.model.payloads.requests.DepositRequest;
import com.vivid.model.payloads.requests.OrderCreateRequest;
import com.vivid.model.dto.OrderDto;
import com.vivid.model.payloads.requests.OrderUpdateRequest;
import com.vivid.model.entity.Article;
import com.vivid.model.entity.ArticleServiceLine;
import com.vivid.model.entity.Feedback;
import com.vivid.model.entity.Order;
import com.vivid.model.entity.Ticket;
import com.vivid.model.entity.User;
import com.vivid.model.enums.OrderStatus;
import com.vivid.model.enums.PaymentStatus;
import com.vivid.model.mapper.OrderMapper;
import com.vivid.repository.ArticleRepository;
import com.vivid.repository.ArticleServiceLineRepository;
import com.vivid.repository.FeedbackRepository;
import com.vivid.repository.OrderRepository;
import com.vivid.repository.TicketRepository;
import com.vivid.repository.UserRepository;
import com.vivid.service.FeedbackService;
import com.vivid.service.OrderService;
import com.vivid.service.ArticleService;
import com.vivid.service.NotificationService;
import com.vivid.service.TicketService;
import com.vivid.common.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ArticleServiceLineRepository articleServiceRepository;
    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;
    private final TicketService ticketService;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackService feedbackService;

    // CORRECTION : ArticleService dépend déjà de OrderService (pour
    // recalculateTotal/recalculateStatus) ; l'injection directe créerait un
    // cycle de dépendances au démarrage de Spring. @Lazy résout le cycle en
    // injectant un proxy dont l'initialisation réelle est différée au
    // premier appel (voir createDeposit(), seul point d'utilisation).
    @Lazy
    private final ArticleService articleService;
    @Value("${vividela.loyalty.point-value}")
    private BigDecimal loyaltyPointValue;
    @Value("${vividela.loyalty.points-per-cfa}")
    private int loyaltyPointsPerCfa;
    @Value("${vividela.loyalty.max-points-per-order}")
    private Integer loyaltyMaxPointsPerOrder;
    @Value("${vividela.ticket.expiration-days}")
    private Integer ticketExpirationDays;

    @Override
    public OrderDto createOrder(OrderCreateRequest request, Long currentUserId) {
        log.debug("Création d'une commande pour le client: {}", request.getUserName());

        User customer = resolveCustomer(request.getUserName(), request.getPhone());

        LocalDate depositDate = LocalDate.now();
        BigDecimal discountAmount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        Integer loyaltyPointsUsed = request.getLoyaltyPointsUsed() != null ? request.getLoyaltyPointsUsed() : 0;

        // Cohérence des dates, la livraison prévue ne peut pas être
        // antérieure au dépôt.
        validateDeliveryDate(depositDate, request.getExpectedDeliveryDate());

        // AJOUT : une commande démarre sans aucune prestation (totalAmount=0),
        // donc toute remise à la création serait nécessairement supérieure au
        // total. Voir revue de code, règle manquante n°14.
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidRequestException(
                    "Impossible d'appliquer une remise à la création de la commande : aucune prestation n'a "
                            + "encore été ajoutée (montant total = 0). Ajoutez d'abord les articles, puis appliquez "
                            + "la remise via la mise à jour de la commande.");
        }

        // AJOUT : les points utilisés ne peuvent pas dépasser le solde réel du
        // client.
        validateLoyaltyPointsUsed(customer, loyaltyPointsUsed);

        Order order = Order.builder()
                .clientUser(customer)
                .depositDate(depositDate)
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .status(OrderStatus.RECEIVED)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .totalAmount(BigDecimal.ZERO)
                .discountAmount(discountAmount)
                .loyaltyPointsUsed(loyaltyPointsUsed)
                .loyaltyProcessed(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(userRepository.findById(currentUserId).orElse(null))
                .updatedBy(userRepository.findById(currentUserId).orElse(null))
                .build();

        Order saved = orderRepository.save(order);
        log.info("Commande créée avec l'ID: {}", saved.getId());

        Ticket ticket = ticketService.ensureTicketForOrder(saved);
        TransactionUtils.runAfterCommit(() -> notificationService.notifyOrderCreated(saved, ticket));

        return toDto(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id, boolean includeArticles) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable avec l'ID: " + id));
        return toDto(order, includeArticles);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        return toDtoList(orderRepository.findAll(), false);
    }

    @Override
    public List<OrderDto> getOrdersByStatuses(List<OrderStatus> statuses) {
        return toDtoList(orderRepository.findByStatusIn(statuses), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByClient(Long clientId) {
        return toDtoList(orderRepository.findByClientUserId(clientId), true);
    }

    @Override
    public OrderDto updateOrder(Long id, OrderUpdateRequest request, Long currentUserId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable: " + id));

        OrderStatus previousStatus = order.getStatus();
        PaymentStatus previousPaymentStatus = order.getPaymentStatus();

        // AJOUT : garde-fou général — une commande livrée, annulée, ou déjà
        // intégralement payée ne doit plus pouvoir être modifiée (hors statut
        // de paiement, ex: remboursement). Voir revue de code, règle
        // manquante n°1.
        boolean orderLocked = previousStatus == OrderStatus.DELIVERED
                || previousStatus == OrderStatus.CANCELLED
                || previousPaymentStatus == PaymentStatus.COMPLETED;

        boolean triesToModifyProtectedFields =
                hasText(request.getUserName()) || hasText(request.getPhone())
                        || request.getDepositDate() != null
                        || request.getExpectedDeliveryDate() != null
                        || request.getShippingAddress() != null
                        || request.getNotes() != null
                        || request.getDiscountAmount() != null
                        || request.getLoyaltyPointsUsed() != null;

        if (orderLocked && triesToModifyProtectedFields) {
            throw new InvalidStateTransitionException(
                    "La commande #" + id + " ne peut plus être modifiée : elle est "
                            + lockedReason(previousStatus, previousPaymentStatus) + ". "
                            + "Seul le statut de paiement (ex : remboursement) reste modifiable.");
        }

        if (hasText(request.getUserName()) || hasText(request.getPhone())) {
            User customer = resolveCustomer(request.getUserName(), request.getPhone());
            order.setClientUser(customer);
        }

        if (request.getDepositDate() != null) {
            order.setDepositDate(request.getDepositDate());
        }
        if (request.getExpectedDeliveryDate() != null) {
            order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        }
        // AJOUT : cohérence des dates. Voir revue de code, règle manquante n°3.
        validateDeliveryDate(order.getDepositDate(), order.getExpectedDeliveryDate());

        // AJOUT : machine à états stricte une fois le stade READY atteint (voir
        // revue de code, règle manquante n°11) — depuis READY, seules les
        // transitions vers DELIVERED ou CANCELLED sont permises ; une fois
        // DELIVERED ou CANCELLED, le statut ne peut plus jamais changer.
        if (request.getStatus() != null) {
                if (previousStatus == OrderStatus.DELIVERED || previousStatus == OrderStatus.CANCELLED) {
                throw new InvalidStateTransitionException(
                        "La commande #" + id + " est " + (previousStatus == OrderStatus.DELIVERED ? "livrée" : "annulée")
                                + " : son statut est définitif et ne peut plus être modifié.");
            } else if (previousStatus == OrderStatus.READY) {
                if (request.getStatus() == OrderStatus.DELIVERED || request.getStatus() == OrderStatus.CANCELLED) {
                    order.setStatus(request.getStatus());
                    if (request.getStatus() == OrderStatus.DELIVERED) {
                        order.setDeliveredAt(LocalDate.now());
                    }
                } else {
                    throw new InvalidStateTransitionException(
                            "Depuis le statut READY, seules les transitions vers DELIVERED (remise au client) "
                                    + "ou CANCELLED (annulation) sont autorisées pour la commande #" + id + ".");
                }
            } else {
                throw new InvalidStateTransitionException(
                        "Le statut de la commande #" + id + " est géré automatiquement en fonction de "
                                + "l'avancement de ses articles tant qu'elle n'a pas atteint le stade READY. "
                                + "Statut actuel : " + previousStatus + ". Pour annuler la commande avant ce "
                                + "stade, utilisez l'action d'annulation dédiée.");
            }
        }

        // AJOUT : paymentStatus ne peut plus être forcé à COMPLETED
        // manuellement : cette valeur est désormais déterminée automatiquement
        // par PaymentServiceImpl lorsque la somme des paiements confirmés
        // couvre le montant net dû. Voir revue de code, règle manquante n°2.
        if (request.getPaymentStatus() != null) {
            if (request.getPaymentStatus() == PaymentStatus.COMPLETED) {
                throw new InvalidStateTransitionException(
                        "Le statut de paiement COMPLETED ne peut plus être positionné manuellement : il est "
                                + "déterminé automatiquement lorsque la somme des paiements confirmés couvre le "
                                + "montant net dû de la commande (voir /payments/{id}/confirm).");
            }
            order.setPaymentStatus(request.getPaymentStatus());
        }

        if (request.getShippingAddress() != null) {
            order.setShippingAddress(request.getShippingAddress());
        }
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }
        if (request.getDiscountAmount() != null) {
            // AJOUT : la remise ne peut pas dépasser le montant total. Voir
            // revue de code, règle manquante n°14 bis.
            if (request.getDiscountAmount().compareTo(order.getTotalAmount()) > 0) {
                throw new InvalidRequestException(
                        "La remise (" + request.getDiscountAmount() + ") ne peut pas dépasser le montant total "
                                + "de la commande (" + order.getTotalAmount() + ").");
            }
            order.setDiscountAmount(request.getDiscountAmount());
        }
        if (request.getLoyaltyPointsUsed() != null) {
            validateLoyaltyPointsUsed(order.getClientUser(), request.getLoyaltyPointsUsed());
            order.setLoyaltyPointsUsed(request.getLoyaltyPointsUsed());
        }
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(userRepository.findById(currentUserId).orElse(null));

        Order saved = orderRepository.save(order);

        // AJOUT : traitement du programme de fidélité au moment de la livraison.
        // Voir revue de code, règle manquante n°12.
        if (saved.getStatus() == OrderStatus.DELIVERED && previousStatus != OrderStatus.DELIVERED) {
            processLoyaltyOnDelivery(saved);
            // AJOUT : dès la livraison, un formulaire d'avis est généré et envoyé
            // au client (voir FeedbackService) ; son avis, une fois soumis, est
            // analysé par Gemini AI puis transmis au manager (et à l'admin si
            // jugé négatif). Voir FeedbackServiceImpl / NotificationServiceImpl.
            feedbackService.requestFeedback(saved);
        }

        if (saved.getStatus() != previousStatus) {
            TransactionUtils.runAfterCommit(() -> notificationService.notifyOrderStatusChangedAsync(saved, previousStatus, saved.getStatus()));
        }
        if (saved.getPaymentStatus() != previousPaymentStatus) {
            TransactionUtils.runAfterCommit(
                    () -> notificationService.
                            notifyPaymentStatusChangedAsync(saved, previousPaymentStatus, saved.getPaymentStatus())
            );
        }

        return toDto(saved, true);
    }

    /**
     * AJOUT : annulation dédiée d'une commande, possible à n'importe quel
     * stade AVANT READY. Voir revue de code, règle manquante n°10.
     */
    @Override
    public OrderDto cancelOrder(Long id, Long currentUserId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable: " + id));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidStateTransitionException(
                    "Impossible d'annuler la commande #" + id + " : elle a déjà été livrée au client.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidStateTransitionException("La commande #" + id + " est déjà annulée.");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(userRepository.findById(currentUserId).orElse(null));

        Order saved = orderRepository.save(order);
        TransactionUtils.runAfterCommit(
                () -> notificationService.notifyOrderStatusChangedAsync(saved, previousStatus, OrderStatus.CANCELLED)
        );

        log.info("Commande {} annulée (statut précédent : {})", id, previousStatus);
        return toDto(saved, true);
    }

    @Override
    public void deleteOrder(Long id) {
        Order o = orderRepository.findById(id).orElseThrow(
                        () -> new ResourceNotFoundException("Commande introuvable: " + id));

        if (o.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new InvalidStateTransitionException(
                    "Impossible de supprimer la commande #" + id + " : elle a déjà été payée. "
                            + "Annulez-la (statut CANCELLED) plutôt que de la supprimer, "
                            + "afin de conserver l'historique comptable.");
        }

        orderRepository.delete(o);
    }

    @Override
    public void recalculateTotal(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Commande introuvable: " + orderId));

        // AJOUT (défense en profondeur) : ArticleServiceImpl bloque déjà toute
        // modification d'article sur une commande livrée/annulée/payée avant
        // d'appeler cette méthode. Voir revue de code, règle manquante n°4.
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED
                || order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new InvalidStateTransitionException(
                    "Impossible de recalculer le montant de la commande #" + orderId + " : elle est "
                            + lockedReason(order.getStatus(), order.getPaymentStatus()) + ".");
        }

        BigDecimal total = articleServiceRepository.sumAppliedPriceByOrderId(orderId);
        order.setTotalAmount(total != null ? total : BigDecimal.ZERO);

        // AJOUT : si le total baisse sous le montant de la remise déjà
        // appliquée, on ramène automatiquement la remise au nouveau total.
        // Voir revue de code, règle manquante n°14 bis.
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(order.getTotalAmount()) > 0) {
            log.warn("Remise de la commande {} ramenée de {} à {} suite au recalcul du total.",
                    orderId, order.getDiscountAmount(), order.getTotalAmount());
            order.setDiscountAmount(order.getTotalAmount());
        }

        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("Montant total recalculé pour la commande {} : {}", orderId, order.getTotalAmount());
    }

    @Override
    public void recalculateStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable: " + orderId));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        List<Article> articles = articleRepository.findByOrderId(orderId);
        if (articles.isEmpty()) {
            return;
        }

        boolean allPending = articles.stream()
                .allMatch(a -> a.getStatus() == ArticleStatus.PENDING);
        boolean allCompleted = articles.stream()
                .allMatch(a -> a.getStatus() == ArticleStatus.COMPLETED);

        OrderStatus newStatus;
        if (allCompleted) {
            newStatus = OrderStatus.READY;
        } else if (allPending) {
            newStatus = OrderStatus.PENDING;
        } else {
            newStatus = OrderStatus.IN_PROGRESS;
        }

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus != newStatus) {
            log.info("Statut de la commande {} recalculé automatiquement : {} → {}",
                    orderId, previousStatus, newStatus);
            order.setStatus(newStatus);
            order.setUpdatedAt(Instant.now());
            Order saved = orderRepository.save(order);

            TransactionUtils.runAfterCommit(
                    () -> notificationService.notifyOrderStatusChangedAsync(saved, previousStatus, newStatus));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getNetAmountDue(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Commande introuvable: " + orderId));
        return computeNetAmountDue(order);
    }

    @Override
    public OrderDto createDeposit(DepositRequest request, Long currentUserId) {
        log.debug("Dépôt atomique d'une commande pour le client ID: {}", request.getClientId());

        User customer = userRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client introuvable avec l'ID: " + request.getClientId()));

        LocalDate depositDate = request.getDepositDate() != null ? request.getDepositDate() : LocalDate.now();
        validateDeliveryDate(depositDate, request.getExpectedDeliveryDate());

        if (request.getArticles() == null || request.getArticles().isEmpty()) {
            throw new InvalidRequestException("Le dépôt doit contenir au moins un vêtement.");
        }

        Order order = Order.builder()
                .clientUser(customer)
                .depositDate(depositDate)
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .status(OrderStatus.RECEIVED)
                .paymentStatus(PaymentStatus.PENDING)
                .notes(request.getNote())
                .totalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .loyaltyPointsUsed(0)
                .loyaltyProcessed(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(userRepository.findById(currentUserId).orElse(null))
                .updatedBy(userRepository.findById(currentUserId).orElse(null))
                .build();

        Order saved = orderRepository.save(order);

        // AJOUT : les articles sont créés dans la MÊME transaction que la
        // commande — si l'un d'eux échoue, toute l'opération est annulée et
        // aucune commande "fantôme" sans article n'est persistée.
        for (var articleDto : request.getArticles()) {
            ArticleCreateRequest articleRequest = ArticleCreateRequest.builder()
                    .clothingType(articleDto.clothingType())
                    .size(articleDto.size())
                    .fabric(articleDto.fabric())
                    .color(articleDto.color())
                    .distinction(articleDto.distinction())
                    .status(articleDto.status() != null ? articleDto.status()
                            : ArticleStatus.PENDING)
                    .services(articleDto.services() != null
                            ? articleDto.services().stream()
                            .map(ArticleServiceDto::service)
                            .collect(Collectors.toList())
                            : List.of())
                    .build();
            articleService.create(articleRequest, saved.getId());
        }

        log.info("Commande #{} déposée avec {} article(s) en une seule transaction",
                saved.getId(), request.getArticles().size());

        Ticket ticket = ticketService.ensureTicketForOrder(saved);
        TransactionUtils.runAfterCommit(() -> notificationService.notifyOrderCreated(saved, ticket));

        return toDto(saved, true);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto findOrderByBarcode(String barcode) {
        Ticket ticket = ticketRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune commande ne correspond au code-barres: " + barcode));
        return toDto(ticket.getOrder(), true);
    }

    // ─── Programme de fidélité ──────────────────────────────────────────────

    private void processLoyaltyOnDelivery(Order order) {
        if (Boolean.TRUE.equals(order.getLoyaltyProcessed())) {
            return;
        }

        User customer = order.getClientUser();
        int pointsUsed = order.getLoyaltyPointsUsed() != null ? order.getLoyaltyPointsUsed() : 0;
        int currentBalance = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;

        int newBalance = currentBalance - pointsUsed;
        if (newBalance < 0) {
            log.warn("Solde de fidélité du client {} insuffisant à la livraison de la commande {} : "
                            + "utilisation de {} points plafonnée au solde disponible ({}).",
                    customer.getId(), order.getId(), pointsUsed, currentBalance);
            newBalance = 0;
        }

        BigDecimal netPaid = computeNetAmountDue(order);
        BigDecimal earnedRaw = netPaid.multiply(BigDecimal.valueOf(loyaltyPointsPerCfa));
        int earned = earnedRaw.setScale(0, RoundingMode.DOWN).intValue();

        customer.setLoyaltyPoints(newBalance + earned);
        userRepository.save(customer);

        order.setLoyaltyProcessed(true);
        orderRepository.save(order);

        log.info("Fidélité — commande {} livrée : {} point(s) déduits, {} point(s) crédités au client {} (nouveau solde: {})",
                order.getId(), pointsUsed, earned, customer.getId(), customer.getLoyaltyPoints());
    }

    private void validateLoyaltyPointsUsed(User customer, Integer pointsUsed) {
        if (pointsUsed == null || pointsUsed <= 0) {
            return;
        }
        int balance = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        if (pointsUsed > balance) {
            throw new InvalidRequestException(
                    "Le client ne dispose que de " + balance + " point(s) de fidélité, "
                            + "impossible d'en utiliser " + pointsUsed + ".");
        }
    }

    private BigDecimal computeNetAmountDue(Order order) {
        BigDecimal net = order.getGrossAmountAfterDiscount();
        int pointsUsed = order.getLoyaltyPointsUsed() != null ? order.getLoyaltyPointsUsed() : 0;
        if (pointsUsed > 0) {
            BigDecimal pointsValue = loyaltyPointValue.multiply(BigDecimal.valueOf(pointsUsed));
            net = net.subtract(pointsValue);
        }
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }

    private void validateDeliveryDate(LocalDate depositDate, LocalDate expectedDeliveryDate) {
        if (depositDate != null && expectedDeliveryDate != null && expectedDeliveryDate.isBefore(depositDate)) {
            throw new InvalidRequestException(
                    "La date de livraison prévue (" + expectedDeliveryDate + ") ne peut pas être antérieure "
                            + "à la date de dépôt (" + depositDate + ").");
        }
    }

    private String lockedReason(OrderStatus status, PaymentStatus paymentStatus) {
        if (status == OrderStatus.DELIVERED) {
            return "livrée";
        }
        if (status == OrderStatus.CANCELLED) {
            return "annulée";
        }
        if (paymentStatus == PaymentStatus.COMPLETED) {
            return "déjà payée intégralement";
        }
        return "verrouillée";
    }

    // ─── Mapper ─────────────────────────────────────────────────────────────

    private OrderDto toDto(Order order, boolean includeArticles) {
        List<Article> articles = articleRepository.findByOrderId(order.getId());
        Map<Long, List<ArticleServiceLine>> servicesByArticle = includeArticles
                ? articles.stream().collect(Collectors.toMap(
                Article::getId,
                a -> articleServiceRepository.findByArticleId(a.getId())))
                : Map.of();
        String ticketNumber = ticketRepository.findByOrderId(order.getId())
                .map(Ticket::getBarcode)
                .orElse(null);
        BigDecimal netAmountDue = computeNetAmountDue(order);
        String feedbackStatus = feedbackRepository.findByOrderId(order.getId())
                .map(this::feedbackStatusLabel)
                .orElse(null);
        return OrderMapper.toDto(order, articles, includeArticles, servicesByArticle, ticketNumber, netAmountDue, feedbackStatus);
    }

    private List<OrderDto> toDtoList(List<Order> orders, boolean includeArticles) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        List<Article> allArticles = articleRepository.findByOrderIdIn(orderIds);
        Map<Long, List<Article>> articlesByOrder = allArticles.stream()
                .collect(Collectors.groupingBy(a -> a.getOrder().getId()));

        Map<Object, List<ArticleServiceLine>> servicesByArticle;
        if (includeArticles && !allArticles.isEmpty()) {
            List<Long> articleIds = allArticles.stream().map(Article::getId).collect(Collectors.toList());
            servicesByArticle = articleServiceRepository.findByArticleIdIn(articleIds).stream()
                    .collect(Collectors.groupingBy(ArticleServiceLine::getArticleId));
        } else {
            servicesByArticle = Map.of();
        }

        Map<Long, String> ticketNumberByOrder = ticketRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(t -> t.getOrder().getId(), Ticket::getBarcode, (a, b) -> a));

        // AJOUT : état du formulaire d'avis (REQUESTED/SUBMITTED), récupéré en
        // une seule requête groupée pour éviter tout N+1 — voir OrderDto.feedbackStatus.
        Map<Long, String> feedbackStatusByOrder = feedbackRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(f -> f.getOrder().getId(), this::feedbackStatusLabel, (a, b) -> a));

        // Calculer les montants nets pour tous les ordres
        Map<Long, BigDecimal> netAmountsByOrder = orders.stream()
                .collect(Collectors.toMap(Order::getId, this::computeNetAmountDue));

        return orders.stream()
                .map(order -> {
                    List<Article> articles = articlesByOrder.getOrDefault(order.getId(), Collections.emptyList());
                    Map<Long, List<ArticleServiceLine>> servicesForThisOrder = includeArticles
                            ? articles.stream().collect(Collectors.toMap(
                            Article::getId,
                            a -> servicesByArticle.getOrDefault(a.getId(), Collections.emptyList())))
                            : Map.of();
                    String ticketNumber = ticketNumberByOrder.get(order.getId());
                    BigDecimal netAmountDue = netAmountsByOrder.get(order.getId());
                    String feedbackStatus = feedbackStatusByOrder.get(order.getId());
                    return OrderMapper.toDto(order, articles, includeArticles, (Map<Long, List<ArticleServiceLine>>) servicesForThisOrder, ticketNumber, netAmountDue, feedbackStatus);
                })
                .collect(Collectors.toList());
    }

    private String feedbackStatusLabel(Feedback feedback) {
        return feedback.getSubmittedAt() != null ? "SUBMITTED" : "REQUESTED";
    }

    private User resolveCustomer(String userName, String phone) {
        if (hasText(userName)) {
            var byUserName = userRepository.findByUserNameIgnoreCase(userName.trim());
            if (byUserName.isPresent()) {
                return byUserName.get();
            }
        }
        if (hasText(phone)) {
            var byPhone = userRepository.findByPhone(phone.trim());
            if (byPhone.isPresent()) {
                return byPhone.get();
            }
        }
        if (!hasText(userName) && !hasText(phone)) {
            throw new ResourceNotFoundException(
                    "Impossible d'identifier le client : fournissez un nom d'utilisateur ou un numéro de téléphone.");
        }
        throw new ResourceNotFoundException(
                "Aucun client trouvé avec le nom d'utilisateur '" + userName + "' ou le téléphone '" + phone + "'.");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
