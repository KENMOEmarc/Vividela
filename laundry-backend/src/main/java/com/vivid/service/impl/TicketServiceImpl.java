package com.vivid.service.impl;

import com.vivid.model.dto.GeneratedPdfDto;
import com.vivid.model.dto.TicketDto;
import com.vivid.model.dto.UserDto;
import com.vivid.model.entity.Article;
import com.vivid.model.entity.ArticleServiceLine;
import com.vivid.model.entity.Order;
import com.vivid.model.entity.Ticket;
import com.vivid.model.enums.PaymentStatus;
import com.vivid.model.enums.TicketStatus;
import com.vivid.exception.ResourceNotFoundException;
import com.vivid.exception.InvalidStateTransitionException;
import com.vivid.model.enums.OrderStatus;
import com.vivid.model.mapper.TicketMapper;
import com.vivid.model.entity.Receipt;
import com.vivid.model.mapper.UserMapper;
import com.vivid.repository.ArticleRepository;
import com.vivid.repository.ArticleServiceLineRepository;
import com.vivid.repository.OrderRepository;
import com.vivid.repository.ReceiptRepository;
import com.vivid.repository.TicketRepository;
import com.vivid.service.TicketService;
import com.vivid.service.NotificationService;
import com.vivid.service.UserService;
import com.vivid.common.pdf.DocumentReferenceGenerator;
import com.vivid.common.pdf.ReceiptPdfGenerator;
import com.vivid.common.pdf.TicketPdfGenerator;
import com.vivid.common.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private static final int MAX_BARCODE_GENERATION_ATTEMPTS = 5;
    private final TicketRepository ticketRepository;
    private final ReceiptRepository receiptRepository;
    private final OrderRepository orderRepository;
    private final ArticleRepository articleRepository;
    private final ArticleServiceLineRepository articleServiceLineRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    @Value("${vividela.ticket.validity-days:90}")
    private int ticketExpirationDays;

    @Override
    public TicketDto generateTicket(Long orderId) {
        Ticket ticket = getOrCreateTicket(orderId);
        return TicketMapper.toDto(ticket);
    }

    @Override
    public GeneratedPdfDto generateTicketPdf(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable avec l'ID: " + orderId));

        List<Article> articles = articleRepository.findByOrderId(orderId);
        if (articles.isEmpty()) {
            throw new InvalidStateTransitionException(
                    "Impossible de générer le ticket : aucun vêtement n'a encore été enregistré pour cette commande.");
        }

        Ticket ticket = getOrCreateTicket(orderId);
        // AJOUT : vérifie l'expiration avant de (re)générer le PDF, et marque
        // le ticket comme consulté.
        ticket = checkExpiryAndMarkDownloaded(ticket);
        log.info("Génération du PDF du ticket pour la commande {}", orderId);
        byte[] pdf = TicketPdfGenerator.generate(order, articles, ticket);
        String reference = ticket.getBarcode() != null ? ticket.getBarcode() : DocumentReferenceGenerator.generateTicketReference(orderId);
        return new GeneratedPdfDto(pdf, reference);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDto getTicketByOrderId(Long orderId) {
        Ticket ticket = ticketRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun ticket n'a encore été généré pour la commande: " + orderId));
        return TicketMapper.toDto(ticket);
    }

    @Override
    public GeneratedPdfDto generateReceiptPdf(Long orderId, String issuerEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable avec l'ID: " + orderId));

        // AJOUT : un reçu ne peut être généré que si la commande a été payée
        // intégralement. Voir revue de code, règle manquante n°1 (section
        // Ticket & Reçu) — exemple explicitement cité par l'utilisateur.
        if (order.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidStateTransitionException(
                    "Impossible de générer le reçu de la commande #" + orderId + " : elle n'a pas encore été "
                            + "payée intégralement (statut de paiement actuel : " + order.getPaymentStatus() + ").");
        }

        List<Article> articles = articleRepository.findByOrderId(orderId);
        if (articles.isEmpty()) {
            throw new InvalidStateTransitionException(
                    "Impossible de générer le reçu : aucun vêtement n'a encore été enregistré pour cette commande.");
        }

        // Récupérer les services appliqués à tous les articles de la commande
        List<ArticleServiceLine> allServices = articles.stream()
                .flatMap(article -> articleServiceLineRepository.findByArticleId(article.getId()).stream())
                .toList();

        // Utilisateur actuellement authentifié qui émet le reçu (affiché sur le
        // document sous "Émis par"). On tolère son absence (ex. appel technique
        // sans contexte utilisateur) sans faire échouer la génération du PDF.
        UserDto issuer = null;
        if (issuerEmail != null && !issuerEmail.isBlank()) {
            try {
                issuer = userService.findByEmail(issuerEmail);
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                log.warn("Utilisateur émetteur introuvable ({}) pour le reçu de la commande {}", issuerEmail, orderId);
            }
        }

        String reference = ensureReceiptForOrder(order, issuerEmail);
        log.info("Génération du PDF du reçu {} pour la commande {}", reference, orderId);
        // AJOUT : le "NET À PAYER" du PDF utilise désormais exactement le
        // même calcul (remise + points de fidélité déduits) que
        // OrderDto.netAmountDue exposé par l'API
        BigDecimal netAmountDue = computeNetAmountDue(order);
        byte[] pdf = ReceiptPdfGenerator.generate(order, articles, allServices, reference, UserMapper.toUser(issuer), netAmountDue);
        return new GeneratedPdfDto(pdf, reference);
    }


    @Override
    public String ensureReceiptForOrder(Order order, String issuerEmail) {
        return receiptRepository.findByOrderId(order.getId())
                .map(Receipt::getReference)
                .orElseGet(() -> createReceipt(order, issuerEmail));
    }

    private String createReceipt(Order order, String issuerEmail) {
        UserDto issuer = null;
        if (issuerEmail != null && !issuerEmail.isBlank()) {
            try {
                issuer = userService.findByEmail(issuerEmail);
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                log.warn("Utilisateur émetteur introuvable ({}) pour la création du reçu de la commande {}",
                        issuerEmail, order.getId());
            }
        }

        Receipt receipt = Receipt.builder()
                .order(order)
                .reference(DocumentReferenceGenerator.generateReceiptReference(order.getId()))
                .issuedAt(Instant.now())
                .issuedBy(UserMapper.toUser(issuer))
                .build();

        try {
            Receipt saved = receiptRepository.save(receipt);
            log.info("Reçu {} généré automatiquement pour la commande {}", saved.getReference(), order.getId());
            return saved.getReference();
        } catch (DataIntegrityViolationException e) {
            // Course entre deux threads (ex : reconciliation automatique +
            // téléchargement manuel simultanés) : l'autre a déjà créé le reçu
            // entre-temps, on relit simplement sa référence plutôt que
            // d'échouer.
            return receiptRepository.findByOrderId(order.getId())
                    .map(Receipt::getReference)
                    .orElseThrow(() -> e);
        }
    }

    /**
     * Montant net dû, remise ET points de fidélité utilisés déduits — même
     * formule que OrderServiceImpl#computeNetAmountDue (dupliquée ici pour
     * éviter une dépendance circulaire TicketService ↔ OrderService, ce
     * dernier dépendant déjà de TicketService pour l'émission automatique
     * du ticket à la création d'une commande).
     */
    private BigDecimal computeNetAmountDue(Order order) {
        BigDecimal net = order.getGrossAmountAfterDiscount();
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }

    @Override
    public Ticket ensureTicketForOrder(Order order) {
        return ticketRepository.findByOrderId(order.getId())
                .orElseGet(() -> {
                    Ticket saved = saveTicketWithRetry(order);
                    log.info("Ticket (code-barre) créé automatiquement à la création de la commande {} : {}",
                            order.getId(), saved.getBarcode());
                    return saved;
                });
    }

    /**
     * Tente de sauvegarder un nouveau ticket, en régénérant le code-barres
     * et en réessayant en cas de {@link DataIntegrityViolationException}
     * (collision sur la contrainte UNIQUE `barcode`).
     */
    private Ticket saveTicketWithRetry(Order order) {
        DataIntegrityViolationException lastError = null;
        for (int attempt = 1; attempt <= MAX_BARCODE_GENERATION_ATTEMPTS; attempt++) {
            try {
                Instant now = Instant.now();
                Ticket ticket = Ticket.builder()
                        .order(order)
                        .barcode(generateBarcode(order))
                        .status(TicketStatus.GENERATED)
                        .issuedAt(now)
                        .expiresAt(now.plus(ticketExpirationDays, ChronoUnit.DAYS))
                        .build();
                return ticketRepository.save(ticket);
            } catch (DataIntegrityViolationException e) {
                lastError = e;
                log.warn("Collision sur le code-barres généré pour la commande {} (tentative {}/{}), nouvelle tentative...",
                        order.getId(), attempt, MAX_BARCODE_GENERATION_ATTEMPTS);
            }
        }
        throw lastError;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * Récupère le ticket existant de la commande, ou le crée s'il n'existe pas encore.
     * La commande doit posséder au moins un vêtement (article) enregistré.
     */
    private Ticket getOrCreateTicket(Long orderId) {
        return ticketRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable avec l'ID: " + orderId));

                    // AJOUT : un ticket de dépôt n'a plus lieu d'être émis pour
                    // une commande annulée. Voir revue de code, règle manquante
                    // n°2 (section Ticket & Reçu).
                    if (order.getStatus() == OrderStatus.CANCELLED) {
                        throw new InvalidStateTransitionException(
                                "Impossible de générer un ticket pour la commande #" + orderId + " : elle est annulée.");
                    }

                    List<Article> articles = articleRepository.findByOrderId(orderId);
                    if (articles.isEmpty()) {
                        throw new InvalidStateTransitionException(
                                "Impossible de générer le ticket : aucun vêtement n'a encore été enregistré pour cette commande.");
                    }

                    Ticket saved = saveTicketWithRetry(order);
                    log.info("Ticket créé avec l'ID {} pour la commande {}", saved.getId(), orderId);

                    // Notify customer about ticket generation
                    TransactionUtils.runAfterCommit(() -> notificationService.notifyTicketGenerated(order, saved));

                    return saved;
                });
    }

    private Ticket checkExpiryAndMarkDownloaded(Ticket ticket) {
        Instant now = Instant.now();

        if (ticket.getStatus() == TicketStatus.EXPIRED
                || (ticket.getExpiresAt() != null && now.isAfter(ticket.getExpiresAt())
                && ticket.getStatus() != TicketStatus.DOWNLOADED)) {
            if (ticket.getStatus() != TicketStatus.EXPIRED) {
                ticket.setStatus(TicketStatus.EXPIRED);
                ticketRepository.save(ticket);
                log.info("Ticket {} de la commande {} marqué EXPIRED (délai de validité dépassé)",
                        ticket.getId(), ticket.getOrder().getId());
            }
            throw new InvalidStateTransitionException(
                    "Ce ticket a expiré (délai de retrait dépassé). Contactez le personnel de la boutique.");
        }

        if (ticket.getStatus() == TicketStatus.GENERATED) {
            ticket.setStatus(TicketStatus.DOWNLOADED);
            ticket.setDownloadedAt(now);
            ticketRepository.save(ticket);
        }

        return ticket;
    }

    private String generateBarcode(Order order) {
        return "TCK" + order.getId() + "-" + (Instant.now().toEpochMilli() % 1_000_000L);
    }
}
