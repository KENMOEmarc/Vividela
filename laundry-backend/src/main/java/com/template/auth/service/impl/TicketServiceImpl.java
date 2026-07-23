package com.template.auth.service.impl;

import com.template.auth.model.dto.GeneratedPdfDto;
import com.template.auth.model.dto.TicketDto;
import com.template.auth.model.entity.Article;
import com.template.auth.model.entity.ArticleService;
import com.template.auth.model.entity.Order;
import com.template.auth.model.entity.Ticket;
import com.template.auth.model.entity.User;
import com.template.auth.model.enums.TicketStatus;
import com.template.auth.exception.ResourceNotFoundException;
import com.template.auth.model.mapper.TicketMapper;
import com.template.auth.model.entity.Receipt;
import com.template.auth.repository.ArticleRepository;
import com.template.auth.repository.ArticleServiceRepository;
import com.template.auth.repository.OrderRepository;
import com.template.auth.repository.ReceiptRepository;
import com.template.auth.repository.TicketRepository;
import com.template.auth.service.TicketService;
import com.template.auth.service.NotificationService;
import com.template.auth.service.UserService;
import com.template.auth.service.pdf.DocumentReferenceGenerator;
import com.template.auth.service.pdf.ReceiptPdfGenerator;
import com.template.auth.service.pdf.TicketPdfGenerator;
import com.template.auth.util.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository           ticketRepository;
    private final ReceiptRepository          receiptRepository;
    private final OrderRepository            orderRepository;
    private final ArticleRepository          articleRepository;
    private final ArticleServiceRepository   articleServiceRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    private final com.template.auth.config.ShopProperties shopProperties;
    private final com.template.auth.config.TicketProperties ticketProperties;
    private final com.template.auth.config.LoyaltyProperties loyaltyProperties;

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
            throw new IllegalArgumentException(
                    "Impossible de générer le ticket : aucun vêtement n'a encore été enregistré pour cette commande.");
        }

        Ticket ticket = getOrCreateTicket(orderId);
        // AJOUT : vérifie l'expiration avant de (re)générer le PDF, et marque
        // le ticket comme consulté. Voir revue de code, règle manquante n°16.
        ticket = checkExpiryAndMarkDownloaded(ticket);
        log.info("Génération du PDF du ticket pour la commande {}", orderId);
        byte[] pdf = TicketPdfGenerator.generate(order, articles, ticket, shopProperties);
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
        if (order.getPaymentStatus() != com.template.auth.model.enums.PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Impossible de générer le reçu de la commande #" + orderId + " : elle n'a pas encore été "
                            + "payée intégralement (statut de paiement actuel : " + order.getPaymentStatus() + ").");
        }

        List<Article> articles = articleRepository.findByOrderId(orderId);
        if (articles.isEmpty()) {
            throw new IllegalArgumentException(
                    "Impossible de générer le reçu : aucun vêtement n'a encore été enregistré pour cette commande.");
        }

        // Récupérer les services appliqués à tous les articles de la commande
        List<ArticleService> allServices = articles.stream()
                .flatMap(article -> articleServiceRepository.findByArticleId(article.getId()).stream())
                .toList();

        // Utilisateur actuellement authentifié qui émet le reçu (affiché sur le
        // document sous "Émis par"). On tolère son absence (ex. appel technique
        // sans contexte utilisateur) sans faire échouer la génération du PDF.
        User issuer = null;
        if (issuerEmail != null && !issuerEmail.isBlank()) {
            try {
                issuer = userService.findByEmail(issuerEmail);
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                log.warn("Utilisateur émetteur introuvable ({}) pour le reçu de la commande {}", issuerEmail, orderId);
            }
        }

        // CORRECTION : la référence du reçu était auparavant re-tirée
        // aléatoirement à chaque appel (DocumentReferenceGenerator.generateReceiptReference),
        // ce qui produisait un numéro de facture différent à chaque
        // téléchargement du même reçu. On réutilise désormais systématiquement
        // la référence stable persistée pour cette commande (créée
        // automatiquement dès le passage à paymentStatus=COMPLETED, ou ici en
        // secours si elle n'existait pas encore). Voir revue de code, règle
        // manquante : "Aucun reçu n'est jamais réellement généré après un paiement".
        String reference = ensureReceiptForOrder(order, issuerEmail);
        log.info("Génération du PDF du reçu {} pour la commande {}", reference, orderId);
        // AJOUT : le "NET À PAYER" du PDF utilise désormais exactement le
        // même calcul (remise + points de fidélité déduits) que
        // OrderDto.netAmountDue exposé par l'API — les deux vues de la
        // commande ne peuvent plus diverger. Voir revue de code, règle
        // manquante n°13.
        java.math.BigDecimal netAmountDue = computeNetAmountDue(order);
        byte[] pdf = ReceiptPdfGenerator.generate(order, articles, allServices, reference, shopProperties, issuer, netAmountDue);
        return new GeneratedPdfDto(pdf, reference);
    }

    /**
     * AJOUT : voir TicketService#ensureReceiptForOrder. Idempotent — si un
     * reçu existe déjà pour cette commande, sa référence est simplement
     * renvoyée telle quelle (jamais de doublon, jamais de nouveau tirage).
     */
    @Override
    public String ensureReceiptForOrder(Order order, String issuerEmail) {
        return receiptRepository.findByOrderId(order.getId())
                .map(Receipt::getReference)
                .orElseGet(() -> createReceipt(order, issuerEmail));
    }

    private String createReceipt(Order order, String issuerEmail) {
        User issuer = null;
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
                .issuedBy(issuer)
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
    private java.math.BigDecimal computeNetAmountDue(Order order) {
        java.math.BigDecimal net = order.getGrossAmountAfterDiscount();
        int pointsUsed = order.getLoyaltyPointsUsed() != null ? order.getLoyaltyPointsUsed() : 0;
        if (pointsUsed > 0) {
            java.math.BigDecimal pointsValue = loyaltyProperties.getPointValue().multiply(java.math.BigDecimal.valueOf(pointsUsed));
            net = net.subtract(pointsValue);
        }
        return net.compareTo(java.math.BigDecimal.ZERO) < 0 ? java.math.BigDecimal.ZERO : net;
    }

    /**
     * Nombre de tentatives en cas de collision sur la colonne UNIQUE `barcode`.
     * Voir revue de code — "Pas de gestion de collision sur barcode/ticket_number".
     * La colonne `barcode` est UNIQUE en base mais generateBarcode() ne
     * vérifie jamais l'unicité au préalable ; en cas de collision (même
     * commande + même milliseconde tronquée), on retente avec un nouveau
     * tirage plutôt que de laisser remonter un 500 générique au client.
     */
    private static final int MAX_BARCODE_GENERATION_ATTEMPTS = 5;

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
                        // AJOUT : date d'expiration calculée à l'émission (voir
                        // revue de code, règle manquante n°16 — un ticket ne
                        // pouvait jamais expirer).
                        .expiresAt(now.plus(ticketProperties.getValidityDays(), java.time.temporal.ChronoUnit.DAYS))
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
                    if (order.getStatus() == com.template.auth.model.enums.OrderStatus.CANCELLED) {
                        throw new IllegalStateException(
                                "Impossible de générer un ticket pour la commande #" + orderId + " : elle est annulée.");
                    }

                    List<Article> articles = articleRepository.findByOrderId(orderId);
                    if (articles.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Impossible de générer le ticket : aucun vêtement n'a encore été enregistré pour cette commande.");
                    }

                    Ticket saved = saveTicketWithRetry(order);
                    log.info("Ticket créé avec l'ID {} pour la commande {}", saved.getId(), orderId);

                    // Notify customer about ticket generation
                    TransactionUtils.runAfterCommit(() -> notificationService.notifyTicketGenerated(order, saved));

                    return saved;
                });
    }

    /**
     * AJOUT : vérifie paresseusement l'expiration d'un ticket à chaque accès
     * (voir revue de code, règle manquante n°16). Un ticket jamais réclamé
     * (toujours GENERATED) dont la date d'expiration est dépassée bascule en
     * EXPIRED et ne peut plus être téléchargé sans intervention du
     * personnel. Un ticket encore valide et accédé pour la première fois
     * bascule en DOWNLOADED.
     */
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
            throw new IllegalStateException(
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
