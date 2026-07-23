package com.template.auth.service.impl;

import com.template.auth.exception.ResourceNotFoundException;
import com.template.auth.model.dto.PaymentDto;
import com.template.auth.model.dto.PaymentRequest;
import com.template.auth.model.entity.Order;
import com.template.auth.model.entity.Payment;
import com.template.auth.model.enums.OrderStatus;
import com.template.auth.model.enums.PaymentStatus;
import com.template.auth.model.mapper.PaymentMapper;
import com.template.auth.repository.OrderRepository;
import com.template.auth.repository.PaymentRepository;
import com.template.auth.repository.UserRepository;
import com.template.auth.service.NotificationService;
import com.template.auth.service.OrderService;
import com.template.auth.service.PaymentService;
import com.template.auth.service.TicketService;
import com.template.auth.util.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final OrderService orderService;
    private final TicketService ticketService;

    /** Statuts de paiement considérés comme "engagés" pour le contrôle de surpaiement. */
    private static final java.util.Set<PaymentStatus> COMMITTED_STATUSES = EnumSet.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED);

    @Override
    public PaymentDto recordPayment(PaymentRequest request, Long currentUserId) {
        log.debug("Enregistrement d'un paiement pour la commande: {}", request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Commande introuvable avec l'ID: " + request.getOrderId()));

        // AJOUT : une commande annulée ne doit plus pouvoir recevoir de
        // paiement. Voir revue de code, règle manquante n°1.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Impossible d'enregistrer un paiement : la commande #" + order.getId() + " est annulée.");
        }

        // AJOUT : montant strictement positif (défense en profondeur, en plus
        // de la validation Bean Validation @DecimalMin sur le DTO). Voir
        // revue de code, règle manquante n°5.
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être strictement positif.");
        }

        // AJOUT : garde-fou anti-doublon — une référence de transaction déjà
        // enregistrée (ex : double clic, webhook rejoué) est refusée plutôt
        // que d'être insérée une seconde fois. Voir revue de code, règle
        // manquante n°6.
        if (request.getTransactionReference() != null && !request.getTransactionReference().isBlank()) {
            paymentRepository.findFirstByTransactionReference(request.getTransactionReference())
                    .ifPresent(existing -> {
                        throw new IllegalStateException(
                                "Un paiement avec la référence de transaction '" + request.getTransactionReference()
                                        + "' a déjà été enregistré (paiement #" + existing.getId() + "). "
                                        + "Doublon refusé.");
                    });
        }

        // AJOUT : contrôle de surpaiement — la somme des paiements déjà
        // engagés (PENDING + COMPLETED) plus ce nouveau paiement ne doit pas
        // dépasser le montant net dû de la commande. Voir revue de code,
        // règle manquante n°2.
        BigDecimal netAmountDue = orderService.getNetAmountDue(order.getId());
        BigDecimal alreadyCommitted = paymentRepository.sumAmountByOrderIdAndStatusIn(order.getId(), COMMITTED_STATUSES);
        BigDecimal newTotal = alreadyCommitted.add(request.getAmount());
        if (newTotal.compareTo(netAmountDue) > 0) {
            BigDecimal remaining = netAmountDue.subtract(alreadyCommitted);
            throw new IllegalArgumentException(
                    "Ce paiement de " + request.getAmount() + " dépasse le montant restant dû sur la commande #"
                            + order.getId() + " (" + (remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO)
                            + " restant sur un total net de " + netAmountDue + ").");
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .payerPhone(request.getPayerPhone())
                .transactionReference(request.getTransactionReference())
                .status(PaymentStatus.PENDING)
                .paidAt(Instant.now())
                .createdBy(currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Paiement enregistré avec l'ID: {} pour la commande: {} (statut: PENDING)", saved.getId(), order.getId());

        TransactionUtils.runAfterCommit(() -> notificationService.notifyPaymentReceived(order, saved));

        return PaymentMapper.toDto(saved);
    }

    /**
     * AJOUT : confirme un paiement PENDING (encaissement effectif) et
     * réconcilie order.paymentStatus. C'est le SEUL endroit qui peut
     * désormais faire basculer une commande à paymentStatus=COMPLETED (voir
     * revue de code, règles manquantes n°3 et n°4 — jusqu'ici le champ
     * Payment.status restait indéfiniment PENDING et order.paymentStatus
     * n'était jamais rapproché des paiements réels).
     */
    @Override
    public PaymentDto confirmPayment(Long paymentId, Long currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable avec l'ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Seul un paiement PENDING peut être confirmé (statut actuel : " + payment.getStatus() + ").");
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        Payment saved = paymentRepository.save(payment);

        reconcileOrderPaymentStatus(payment.getOrder().getId());

        log.info("Paiement {} confirmé (COMPLETED) pour la commande {}", paymentId, payment.getOrder().getId());
        return PaymentMapper.toDto(saved);
    }

    /**
     * AJOUT : marque un paiement PENDING comme FAILED (ex : chèque rejeté).
     * Voir revue de code, règle manquante n°4.
     */
    @Override
    public PaymentDto failPayment(Long paymentId, Long currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable avec l'ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Seul un paiement PENDING peut être marqué comme échoué (statut actuel : " + payment.getStatus() + ").");
        }

        payment.setStatus(PaymentStatus.FAILED);
        Payment saved = paymentRepository.save(payment);

        log.info("Paiement {} marqué comme échoué (FAILED) pour la commande {}", paymentId, payment.getOrder().getId());
        return PaymentMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(PaymentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Recalcule order.paymentStatus à partir de la somme des paiements
     * COMPLETED de la commande, comparée au montant net dû :
     *  - somme COMPLETED >= net dû → COMPLETED (soldée)
     *  - sinon (paiement partiel ou aucun paiement confirmé) → reste PENDING
     * Ne touche jamais un statut déjà FAILED/REFUNDED positionné manuellement
     * par le personnel (ex : remboursement effectué).
     */
    private void reconcileOrderPaymentStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable: " + orderId));

        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        BigDecimal netAmountDue = orderService.getNetAmountDue(orderId);
        BigDecimal completedTotal = paymentRepository.sumAmountByOrderIdAndStatusIn(
                orderId, EnumSet.of(PaymentStatus.COMPLETED));

        PaymentStatus previous = order.getPaymentStatus();
        PaymentStatus newStatus = completedTotal.compareTo(netAmountDue) >= 0 && netAmountDue.compareTo(BigDecimal.ZERO) > 0
                ? PaymentStatus.COMPLETED
                : PaymentStatus.PENDING;

        if (newStatus != previous) {
            order.setPaymentStatus(newStatus);
            order.setUpdatedAt(Instant.now());
            Order saved = orderRepository.save(order);
            TransactionUtils.runAfterCommit(() -> notificationService.notifyPaymentStatusChangedAsync(saved, previous, newStatus));
            log.info("Statut de paiement de la commande {} réconcilié automatiquement : {} → {} "
                            + "(payé: {}, net dû: {})",
                    orderId, previous, newStatus, completedTotal, netAmountDue);

            // AJOUT : dès que la commande est intégralement payée, on génère
            // (et persiste) immédiatement la référence de son reçu — voir
            // revue de code, règle manquante : "Aucun reçu n'est jamais
            // réellement généré après un paiement". Le PDF lui-même reste
            // rendu à la volée au téléchargement (voir
            // TicketServiceImpl#generateReceiptPdf), mais son numéro de
            // facture est désormais fixé dès cet instant et ne varie plus
            // d'un téléchargement à l'autre. Best-effort : un échec ici ne
            // doit jamais faire échouer la réconciliation du paiement.
            if (newStatus == PaymentStatus.COMPLETED) {
                // BUGFIX : l'appel à ensureReceiptForOrder() partageait la même
                // transaction que confirmPayment(). Si la génération du reçu
                // échouait, la transaction était marquée comme rollback-only,
                // causant le rollback silencieux de la confirmation de paiement.
                // On délègue maintenant cette opération à un post-commit pour
                // l'exécuter APRÈS la validation du paiement.
                TransactionUtils.runAfterCommit(() -> {
                    try {
                        String reference = ticketService.ensureReceiptForOrder(saved, null);
                        log.info("Reçu {} disponible pour la commande {} (générée automatiquement après paiement complet)",
                                reference, orderId);
                    } catch (Exception e) {
                        log.error("Échec de la génération automatique du reçu pour la commande {}", orderId, e);
                    }
                });
            }
        }
    }
}
