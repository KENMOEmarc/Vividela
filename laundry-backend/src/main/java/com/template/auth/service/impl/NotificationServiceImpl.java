package com.template.auth.service.impl;

import com.template.auth.model.entity.Feedback;
import com.template.auth.model.entity.Notification;
import com.template.auth.model.entity.Order;
import com.template.auth.model.entity.Payment;
import com.template.auth.model.entity.Product;
import com.template.auth.model.entity.Ticket;
import com.template.auth.model.entity.User;
import com.template.auth.model.enums.FeedbackSentiment;
import com.template.auth.model.enums.NotificationType;
import com.template.auth.model.enums.OrderStatus;
import com.template.auth.model.enums.PaymentStatus;
import com.template.auth.model.enums.RequestStatus;
import com.template.auth.repository.NotificationRepository;
import com.template.auth.repository.UserRepository;
import com.template.auth.service.EmailSender;
import com.template.auth.service.NotificationService;
import com.template.auth.service.SmsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final Clock clock;

    // Unique bean de type Executor dans l'application (voir
    // config.NotificationExecutorConfig) : l'autowiring par type suffit, pas
    // besoin de @Qualifier.
    private final Executor notificationExecutor;

    // ── Reprise automatique des envois EMAIL/SMS en échec ──────────────────
    // Voir revue de code — "Statut des envois email/SMS non exposé/exploité
    // côté supervision" : jusqu'ici, un échec d'envoi (SMTP non configuré,
    // numéro invalide, timeout Twilio...) restait silencieux, sans retry ni
    // alerte. Les constantes ci-dessous sont configurables via
    // application.yml (voir section "notifications.retry").

    /** Nombre maximum de tentatives (au-delà de l'envoi initial) avant abandon. */
    @Value("${notifications.retry.max-attempts:3}")
    private int maxRetryAttempts;

    /** Nombre maximum de notifications retentées par exécution du job. */
    @Value("${notifications.retry.batch-size:100}")
    private int retryBatchSize;

    @Override
    public void notifyOrderCreated(Order order, Ticket ticket) {
        // Résolution EAGER du client sur le thread appelant (encore dans la
        // transaction d'origine) : voir notifyOrderStatusChangedAsync pour
        // l'explication du risque de LazyInitializationException sinon.
        User user = order.getClientUser();
        Long orderId = order.getId();

        String subject = "Votre commande a été créée (#" + orderId + ")";
        StringBuilder body = new StringBuilder("Détails: dépôt le ")
                .append(order.getDepositDate())
                .append(". Montant estimé: ")
                .append(order.getTotalAmount());
        if (order.getExpectedDeliveryDate() != null) {
            body.append(". Livraison prévue le ").append(order.getExpectedDeliveryDate());
        }
        if (ticket != null && ticket.getBarcode() != null) {
            body.append(". Code-barre de votre commande: ").append(ticket.getBarcode());
        }
        String message = body.toString();

        dispatchAsync(() -> createAndSend(user, order, subject, message),
                "commande créée #" + orderId);
    }

    @Override
    public void notifyTicketGenerated(Order order, Ticket ticket) {
        User user = order.getClientUser();
        Long orderId = order.getId();
        String subject = "Ticket généré — commande #" + orderId;
        String body = "Votre ticket a été généré. Code-barre: " + ticket.getBarcode() + ". Détails de la commande disponibles en ligne.";

        dispatchAsync(() -> createAndSend(user, order, subject, body),
                "ticket généré commande #" + orderId);
    }

    @Override
    public void notifyAllArticlesSameState(Order order, String stateLabel) {
        User user = order.getClientUser();
        Long orderId = order.getId();
        String subject = "Tous les vêtements ont changé d'état";
        String body = "Tous les vêtements de votre commande #" + orderId + " sont maintenant : " + stateLabel;

        // Notification interne uniquement (pas d'email/SMS pour ce statut intermédiaire)
        dispatchAsync(
                () -> recordNotification(user, order, subject, body, NotificationType.IN_APP, RequestStatus.SUCCESS),
                "articles même état commande #" + orderId);
    }

    @Override
    public void notifyAllArticlesReady(Order order) {
        User user = order.getClientUser();
        Long orderId = order.getId();
        String subject = "Votre commande est prête — #" + orderId;
        String body = "Tous les vêtements de votre commande sont prêts à être récupérés.";

        dispatchAsync(() -> createAndSend(user, order, subject, body),
                "articles prêts commande #" + orderId);
    }

    @Override
    public void notifyPaymentReceived(Order order, Payment payment) {
        User user = order.getClientUser();
        Long orderId = order.getId();
        String subject = "Paiement reçu — commande #" + orderId;
        String body = "Paiement de " + payment.getAmount() + " reçu. Référence: " + payment.getTransactionReference();

        dispatchAsync(() -> createAndSend(user, order, subject, body),
                "paiement reçu commande #" + orderId);
    }

    @Override
    public void notifyClientCreated(User client) {
        String subject = "Bienvenue chez Vividela";
        String body = "Bonjour " + client.getFirstName() + ", un compte vous a été créé. "
                + "Identifiant : " + client.getUserName() + ". "
                + "Utilisez la procédure \"mot de passe oublié\" pour définir votre mot de passe.";

        dispatchAsync(() -> {
            recordNotification(client, null, subject, body, NotificationType.IN_APP, RequestStatus.SUCCESS);
            sendEmailChannel(client, null, subject, body);
            sendSmsChannel(client, null, subject, body);
            log.info("Notification de création de compte envoyée au client ID: {}", client.getId());
        }, "création de compte client #" + client.getId());
    }

    @Override
    public void notifyLowStock(Product product, java.math.BigDecimal currentTotalQuantity) {
        String subject = "⚠️ Stock en alerte — " + product.getName();
        String body = "Le stock du produit \"" + product.getName() + "\" est bas : "
                + currentTotalQuantity + " " + product.getMeasurementUnit()
                + " restant(s) (seuil d'alerte : " + product.getThresholdValue() + " "
                + product.getMeasurementUnit() + ").";

        // Alerte interne : concerne le personnel (ADMIN/EMPLOYEE), pas un client.
        // Lecture de la liste (staff) faite EAGER sur le thread appelant ; l'envoi
        // à chaque destinataire est ensuite parallélisé sur notificationExecutor
        // (1 CompletableFuture par membre du staff), comme pour les avis clients.
        List<User> staff = userRepository.findByRoleIn(List.of("ADMIN", "EMPLOYEE"));
        for (User u : staff) {
            dispatchAsync(() -> {
                recordNotification(u, null, subject, body, NotificationType.IN_APP, RequestStatus.SUCCESS);
                sendEmailChannel(u, null, subject, body);
                sendSmsChannel(u, null, subject, body);
            }, "alerte stock bas '" + product.getName() + "' → user #" + u.getId());
        }
        log.info("Alerte stock bas déclenchée (asynchrone) pour {} membre(s) du personnel — produit '{}' (qté={})",
                staff.size(), product.getName(), currentTotalQuantity);
    }

    @Override
    public CompletableFuture<Void> notifyOrderStatusChangedAsync(Order order, OrderStatus previousStatus, OrderStatus newStatus) {
        if (newStatus == null || newStatus == previousStatus) {
            // Rien n'a réellement changé : on ne notifie pas.
            return CompletableFuture.completedFuture(null);
        }

        // IMPORTANT : on résout le client MAINTENANT (thread appelant, encore
        // dans la transaction de l'action qui a modifié la commande), pas dans
        // le thread asynchrone plus bas — sinon l'association LAZY
        // Order.clientUser risque une LazyInitializationException une fois la
        // transaction d'origine terminée.
        User client = order.getClientUser();
        Long orderId = order.getId();

        String subject = "Statut de votre commande mis à jour — #" + orderId;
        String body = "Le statut de votre commande #" + orderId + " est passé de "
                + orderStatusLabel(previousStatus) + " à " + orderStatusLabel(newStatus) + ".";

        log.info("Changement de statut détecté pour la commande {} : {} → {} — déclenchement des notifications asynchrones",
                orderId, previousStatus, newStatus);

        // 1 CompletableFuture dédiée à la notification IN_APP : toujours envoyée,
        // quel que soit le nouveau statut.
        CompletableFuture<Void> inAppFuture = CompletableFuture.runAsync(
                        () -> recordNotification(client, order, subject, body, NotificationType.IN_APP, RequestStatus.SUCCESS),
                        notificationExecutor)
                .exceptionally(ex -> {
                    log.error("Échec de l'envoi de la notification IN_APP (commande {}, {} → {})",
                            orderId, previousStatus, newStatus, ex);
                    return null;
                });

        // Commande "terminée" (READY, tous les articles traités) ou "livrée"
        // (DELIVERED) : le client reçoit en plus un email, sur sa propre
        // CompletableFuture (thread séparé de celle de l'IN_APP).
        boolean isTerminalEvent = newStatus == OrderStatus.READY || newStatus == OrderStatus.DELIVERED;
        if (!isTerminalEvent) {
            return inAppFuture;
        }

        CompletableFuture<Void> emailFuture = CompletableFuture.runAsync(
                        () -> sendEmailChannel(client, order, subject, body),
                        notificationExecutor)
                .exceptionally(ex -> {
                    log.error("Échec de l'envoi de l'email de notification (commande {}, statut {})",
                            orderId, newStatus, ex);
                    return null;
                });

        return CompletableFuture.allOf(inAppFuture, emailFuture);
    }

    @Override
    public CompletableFuture<Void> notifyPaymentStatusChangedAsync(Order order, PaymentStatus previousStatus, PaymentStatus newStatus) {
        if (newStatus != PaymentStatus.COMPLETED || newStatus == previousStatus) {
            // Seule la transition VERS "payée" (COMPLETED) déclenche une notification ici.
            return CompletableFuture.completedFuture(null);
        }

        User client = order.getClientUser();
        Long orderId = order.getId();

        String subject = "Paiement confirmé — commande #" + orderId;
        String body = "Bonne nouvelle : votre commande #" + orderId + " est maintenant marquée comme payée. Merci !";

        log.info("Commande {} marquée comme payée ({} → {}) — déclenchement des notifications asynchrones",
                orderId, previousStatus, newStatus);

        CompletableFuture<Void> inAppFuture = CompletableFuture.runAsync(
                        () -> recordNotification(client, order, subject, body, NotificationType.IN_APP, RequestStatus.SUCCESS),
                        notificationExecutor)
                .exceptionally(ex -> {
                    log.error("Échec de l'envoi de la notification IN_APP de paiement (commande {})", orderId, ex);
                    return null;
                });

        CompletableFuture<Void> emailFuture = CompletableFuture.runAsync(
                        () -> sendEmailChannel(client, order, subject, body),
                        notificationExecutor)
                .exceptionally(ex -> {
                    log.error("Échec de l'envoi de l'email de confirmation de paiement (commande {})", orderId, ex);
                    return null;
                });

        return CompletableFuture.allOf(inAppFuture, emailFuture);
    }

    /**
     * Libellé humain (FR) d'un statut de commande, utilisé dans le corps des
     * notifications de changement de statut.
     */
    private String orderStatusLabel(OrderStatus status) {
        if (status == null) {
            return "inconnu";
        }
        return switch (status) {
            case RECEIVED -> "reçue";
            case PENDING -> "en attente de traitement";
            case IN_PROGRESS -> "en cours de traitement";
            case READY -> "terminée";
            case DELIVERED -> "livrée";
            case CANCELLED -> "annulée";
        };
    }

    @Override
    public void notifyFeedbackRequested(Order order) {
        User user = order.getClientUser();
        Long orderId = order.getId();
        String subject = "Donnez votre avis — commande #" + orderId;
        String body = "Votre commande #" + orderId + " a été livrée. Votre avis nous aide à améliorer "
                + "nos services : prenez un instant pour le partager depuis votre espace client.";

        dispatchAsync(() -> createAndSend(user, order, subject, body),
                "demande d'avis commande #" + orderId);
    }

    @Override
    public CompletableFuture<Void> notifyFeedbackReceived(Feedback feedback) {
        Order order = feedback.getOrder();
        Long orderId = order.getId();
        String subject = "Nouvel avis client — commande #" + orderId;
        String body = buildFeedbackNotificationBody(feedback, order);

        List<User> managers = userRepository.findByRoleIn(List.of("MANAGER"));
        if (managers.isEmpty()) {
            log.info("Avis client (commande {}) : aucun manager à notifier.", orderId);
            return CompletableFuture.completedFuture(null);
        }

        // 1 CompletableFuture par manager, sur le pool dédié : l'envoi à
        // plusieurs destinataires se fait en parallèle plutôt qu'en boucle
        // séquentielle sur un seul thread.
        List<CompletableFuture<Void>> perManagerFutures = managers.stream()
                .map(manager -> CompletableFuture.runAsync(() -> {
                            recordNotification(manager, order, subject, body, NotificationType.IN_APP, RequestStatus.SUCCESS);
                            sendEmailChannel(manager, order, subject, body);
                        }, notificationExecutor)
                        .exceptionally(ex -> {
                            log.error("Échec de la notification d'avis client au manager {} (commande {})",
                                    manager.getId(), orderId, ex);
                            return null;
                        }))
                .toList();

        return CompletableFuture.allOf(perManagerFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Avis client (commande {}, sentiment={}) transmis à {} manager(s)",
                        orderId, feedback.getSentiment(), managers.size()));
    }

    @Override
    public CompletableFuture<Void> notifyFeedbackNegative(Feedback feedback) {
        Order order = feedback.getOrder();
        Long orderId = order.getId();
        String subject = "⚠️ Avis client négatif — commande #" + orderId;
        String body = buildFeedbackNotificationBody(feedback, order);

        List<User> admins = userRepository.findByRoleIn(List.of("ADMIN"));
        if (admins.isEmpty()) {
            log.warn("Avis client négatif (commande {}) : aucun administrateur à notifier.", orderId);
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> perAdminFutures = admins.stream()
                .map(admin -> CompletableFuture.runAsync(() -> {
                            recordNotification(admin, order, subject, body, NotificationType.IN_APP, RequestStatus.SUCCESS);
                            sendEmailChannel(admin, order, subject, body);
                        }, notificationExecutor)
                        .exceptionally(ex -> {
                            log.error("Échec de la notification d'avis négatif à l'administrateur {} (commande {})",
                                    admin.getId(), orderId, ex);
                            return null;
                        }))
                .toList();

        return CompletableFuture.allOf(perAdminFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.warn("Avis client négatif (commande {}) transmis à {} administrateur(s)",
                        orderId, admins.size()));
    }

    /**
     * Corps commun aux notifications manager (toujours) et admin (avis
     * négatifs uniquement) : identité du client, note, sentiment détecté par
     * l'IA, commentaire brut et résumé IA (quand disponibles).
     */
    private String buildFeedbackNotificationBody(Feedback feedback, Order order) {
        StringBuilder body = new StringBuilder("Le client ")
                .append(clientLabel(order))
                .append(" a laissé une note de ").append(feedback.getRating()).append("/5")
                .append(" pour la commande #").append(order.getId())
                .append(" (sentiment détecté par l'IA : ").append(sentimentLabel(feedback.getSentiment())).append(")");

        if (feedback.getComment() != null && !feedback.getComment().isBlank()) {
            body.append(". Commentaire : \"").append(feedback.getComment()).append("\"");
        }
        if (feedback.getAiSummary() != null && !feedback.getAiSummary().isBlank()) {
            body.append(". Résumé IA : ").append(feedback.getAiSummary());
        }
        return body.toString();
    }

    private String clientLabel(Order order) {
        User client = order.getClientUser();
        if (client == null) {
            return "inconnu";
        }
        return (client.getFirstName() + " " + client.getLastName()).trim();
    }

    private String sentimentLabel(FeedbackSentiment sentiment) {
        if (sentiment == null) {
            return "non déterminé";
        }
        return switch (sentiment) {
            case POSITIVE -> "positif";
            case NEUTRAL -> "neutre";
            case NEGATIVE -> "négatif";
        };
    }

    // ─── Reprise & escalade des envois EMAIL/SMS en échec ─────────────────────

    /**
     * Job planifié : retente l'envoi des notifications EMAIL/SMS en échec
     * dont le nombre de tentatives n'a pas encore atteint
     * {@code maxRetryAttempts}.
     * <p>
     * Exécuté périodiquement (voir {@code notifications.retry.fixed-delay-ms}).
     * La requête de sélection s'exécute dans la transaction de cette méthode
     * (invoquée par le scheduler à travers le proxy Spring, donc soumise à
     * {@code @Transactional} au niveau classe) ; les données nécessaires à
     * l'envoi (email/téléphone du destinataire, sujet, message) sont
     * résolues EAGER ici, puis chaque tentative d'envoi réel est déléguée à
     * {@link #dispatchAsync}, comme tout le reste de ce service — jamais
     * exécutée sur le thread du scheduler.
     */
    @Scheduled(fixedDelayString = "${notifications.retry.fixed-delay-ms:300000}",
               initialDelayString = "${notifications.retry.initial-delay-ms:60000}")
    public void retryFailedChannelNotifications() {
        List<Notification> candidates = notificationRepository.findFailedForRetry(
                List.of(NotificationType.EMAIL, NotificationType.SMS),
                maxRetryAttempts,
                PageRequest.of(0, retryBatchSize));

        if (candidates.isEmpty()) {
            return;
        }

        log.info("Reprise planifiée : {} notification(s) EMAIL/SMS en échec à retenter (retryCount < {})",
                candidates.size(), maxRetryAttempts);

        for (Notification candidate : candidates) {
            Long notificationId = candidate.getId();
            NotificationType type = candidate.getNotificationType();
            String subject = candidate.getSubject();
            String message = candidate.getMessage();
            User user = candidate.getUser();
            String email = user != null ? user.getEmail() : null;
            String phone = user != null ? user.getPhone() : null;
            int previousRetryCount = candidate.getRetryCount() != null ? candidate.getRetryCount() : 0;

            dispatchAsync(
                    () -> retrySingleNotification(notificationId, type, subject, message, email, phone, previousRetryCount),
                    "reprise notification #" + notificationId);
        }
    }

    /**
     * Exécute une tentative de reprise unique, hors de toute transaction
     * d'origine (voir {@link #dispatchAsync}) : recharge la notification par
     * son id pour la mettre à jour, plutôt que de réutiliser l'entité
     * chargée sur le thread du scheduler (qui serait détachée ici).
     */
    private void retrySingleNotification(Long notificationId, NotificationType type, String subject, String message,
                                          String email, String phone, int previousRetryCount) {
        boolean sent = switch (type) {
            case EMAIL -> email != null && !email.isBlank() && emailSender.sendEmail(email, subject, message);
            case SMS -> phone != null && !phone.isBlank() && smsSender.sendSms(phone, subject + " - " + message);
            default -> false;
        };

        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setStatus(sent ? RequestStatus.SUCCESS : RequestStatus.FAILED);
            n.setRetryCount(previousRetryCount + 1);
            n.setLastAttemptAt(Instant.now(clock));
            notificationRepository.save(n);
        });

        log.info("Reprise notification {} #{} — résultat : {}", type, notificationId, sent ? "SUCCESS" : "FAILED");
    }

    /**
     * Job planifié : signale au personnel (ADMIN/MANAGER) les notifications
     * EMAIL/SMS ayant épuisé toutes leurs tentatives de reprise sans jamais
     * aboutir — échec définitif qui, avant ce mécanisme, restait totalement
     * silencieux (voir revue de code, point d'origine de cette fonctionnalité).
     * <p>
     * Chaque notification concernée est marquée {@code escalated=true} pour
     * ne pas être re-signalée à l'exécution suivante du job.
     */
    @Scheduled(cron = "${notifications.retry.escalation-cron:0 */15 * * * *}")
    public void escalateExhaustedNotificationFailures() {
        List<Notification> exhausted = notificationRepository.findExhaustedNotEscalated(
                List.of(NotificationType.EMAIL, NotificationType.SMS), maxRetryAttempts);

        if (exhausted.isEmpty()) {
            return;
        }

        log.warn("{} notification(s) EMAIL/SMS en échec définitif (retries épuisés) — alerte du personnel",
                exhausted.size());

        String subject = "⚠️ Échecs d'envoi de notifications clients";
        String body = exhausted.size() + " notification(s) (email/SMS) n'ont pas pu être délivrées après "
                + maxRetryAttempts + " tentative(s) de reprise. Vérifiez la configuration SMTP/Twilio ainsi que "
                + "les coordonnées des clients concernés (rubrique Notifications du back-office).";

        List<User> staff = userRepository.findByRoleIn(List.of("ADMIN", "MANAGER"));
        for (User u : staff) {
            dispatchAsync(() -> {
                recordNotification(u, null, subject, body, NotificationType.IN_APP, RequestStatus.SUCCESS);
                sendEmailChannel(u, null, subject, body);
            }, "alerte échecs notifications → user #" + u.getId());
        }

        exhausted.forEach(n -> n.setEscalated(true));
        notificationRepository.saveAll(exhausted);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * Enregistre la notification "in-app" (toujours un succès : c'est une
     * simple écriture en base), puis tente l'email et le SMS pour le client
     * de la commande, chacun étant enregistré avec son statut RÉEL
     * (SUCCESS/FAILED), au lieu du statut SUCCESS optimiste précédent.
     * Voir revue de code — "Statut de notification toujours SUCCESS".
     * <p>
     * {@code user} doit être résolu par l'appelant AVANT de basculer sur le
     * thread asynchrone (voir {@link #dispatchAsync}) : cette méthode est
     * systématiquement exécutée sur notificationExecutor, hors de toute
     * transaction/session Hibernate d'origine, donc plus aucun accès LAZY
     * n'est possible ici.
     */
    private void createAndSend(User user, Order order, String subject, String message) {
        recordNotification(user, order, subject, message, NotificationType.IN_APP, RequestStatus.SUCCESS);
        sendEmailChannel(user, order, subject, message);
        sendSmsChannel(user, order, subject, message);
    }

    /**
     * Point d'entrée UNIQUE pour tout envoi de notification asynchrone dans
     * ce service : toutes les méthodes publiques (déclencheurs "instantanés"
     * comme les déclencheurs déjà multi-canaux) passent désormais par ici,
     * de sorte qu'AUCUN envoi de notification (IN_APP, email ou SMS) ne
     * s'exécute plus sur le thread appelant (thread de la requête HTTP ou
     * thread transactionnel). {@code task} doit donc capturer uniquement des
     * données déjà résolues (entités/valeurs lues sur le thread appelant),
     * jamais déclencher de nouvel accès LAZY.
     */
    private CompletableFuture<Void> dispatchAsync(Runnable task, String errorContext) {
        return CompletableFuture.runAsync(task, notificationExecutor)
                .exceptionally(ex -> {
                    log.error("Échec de l'envoi de notification asynchrone ({})", errorContext, ex);
                    return null;
                });
    }

    private void sendEmailChannel(User user, Order order, String subject, String message) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        boolean sent = emailSender.sendEmail(user.getEmail(), subject, message);
        recordNotification(user, order, subject, message, NotificationType.EMAIL,
                sent ? RequestStatus.SUCCESS : RequestStatus.FAILED);
    }

    private void sendSmsChannel(User user, Order order, String subject, String message) {
        if (user == null || user.getPhone() == null || user.getPhone().isBlank()) {
            return;
        }
        boolean sent = smsSender.sendSms(user.getPhone(), subject + " - " + message);
        recordNotification(user, order, subject, message, NotificationType.SMS,
                sent ? RequestStatus.SUCCESS : RequestStatus.FAILED);
    }

    private void recordNotification(User user, Order order, String subject, String message,
                                     NotificationType type, RequestStatus status) {
        Notification n = Notification.builder()
                .user(user)
                .order(order)
                .subject(subject)
                .message(message)
                .notificationType(type)
                .status(status)
                .sentAt(Instant.now(clock))
                .build();

        notificationRepository.save(n);
        log.info("Notification {} enregistrée (statut={}) pour user={} order={} subject={}",
                type, status,
                user != null ? user.getId() : null,
                order != null ? order.getId() : null, subject);
    }
}
