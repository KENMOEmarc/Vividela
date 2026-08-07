package com.vivid.service;

import com.vivid.model.entity.Feedback;
import com.vivid.model.entity.Order;
import com.vivid.model.entity.Payment;
import com.vivid.model.entity.Product;
import com.vivid.model.entity.Ticket;
import com.vivid.model.entity.User;
import com.vivid.model.enums.OrderStatus;
import com.vivid.model.enums.PaymentStatus;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {
    void notifyOrderCreated(Order order, Ticket ticket);

    void notifyTicketGenerated(Order order, Ticket ticket);

    void notifyAllArticlesSameState(Order order, String stateLabel);

    void notifyAllArticlesReady(Order order);

    void notifyPaymentReceived(Order order, Payment payment);

    /**
     * Notifie un client (email + SMS si disponibles, + notification in-app)
     * que son compte vient d'être créé, par exemple depuis le dashboard
     * admin (page "Clients"). Voir revue de code — "createCustomer ne
     * notifie jamais le client créé".
     */
    void notifyClientCreated(User client);

    /**
     * Notifie le personnel (ADMIN/EMPLOYEE) qu'un produit vient de passer
     * en dessous (ou au niveau) de son seuil d'alerte de stock.
     */
    void notifyLowStock(Product product, java.math.BigDecimal currentTotalQuantity);

    /**
     * Notifie le client qu'un changement de statut de sa commande vient
     * d'avoir lieu (ex : RECEIVED → PENDING, IN_PROGRESS → READY, etc.).
     * L'appelant doit d'abord effectuer et persister le changement de statut
     * (l'action), puis appeler cette méthode : chaque canal de notification
     * (IN_APP, puis email si applicable) est envoyé sur sa propre
     * {@link CompletableFuture}, exécutée sur un pool dédié, sans bloquer le
     * thread appelant.
     * <p>
     * Comportement :
     * - Tout changement de statut déclenche toujours une notification IN_APP.
     * - Si le nouveau statut correspond à une commande "terminée" (READY) ou
     * "livrée" (DELIVERED), le client reçoit en plus un email.
     * - Aucune notification n'est envoyée si le statut n'a pas changé.
     *
     * @return une CompletableFuture qui se termine une fois tous les canaux
     * concernés envoyés (utile pour les tests ; l'appelant de
     * production peut l'ignorer, l'envoi étant asynchrone).
     */
    CompletableFuture<Void> notifyOrderStatusChangedAsync(Order order, OrderStatus previousStatus, OrderStatus newStatus);

    /**
     * Notifie le client que le statut de paiement de sa commande vient de
     * passer à "payée" (COMPLETED). Comme pour
     * {@link #notifyOrderStatusChangedAsync}, l'action (persistance du
     * nouveau statut de paiement) doit déjà avoir eu lieu avant l'appel, et
     * chaque canal (IN_APP puis email) est envoyé via sa propre
     * {@link CompletableFuture} sur un pool dédié.
     * <p>
     * Seule la transition VERS PaymentStatus.COMPLETED déclenche une
     * notification ; les autres transitions (PENDING, FAILED, REFUNDED) n'en
     * déclenchent pas ici.
     */
    CompletableFuture<Void> notifyPaymentStatusChangedAsync(Order order, PaymentStatus previousStatus, PaymentStatus newStatus);

    /**
     * Envoie au client le formulaire d'avis à remplir, juste après que sa
     * commande a été marquée comme livrée (voir FeedbackServiceImpl#requestFeedback).
     * Envoyée en IN_APP + email, comme les autres notifications "terminales"
     * de commande (notifyAllArticlesReady, notifyPaymentReceived...).
     */
    void notifyFeedbackRequested(Order order);

    /**
     * Notifie chaque MANAGER qu'un client vient de soumettre un avis sur une
     * commande livrée, une fois l'avis analysé par l'IA (sentiment + résumé
     * inclus dans le corps de la notification).
     * <p>
     * Une {@link CompletableFuture} indépendante est déclenchée par
     * destinataire (manager) sur le pool {@code notificationExecutor}, afin
     * que l'envoi à N managers ne soit jamais sérialisé sur un seul thread.
     * L'appelant (voir FeedbackAnalysisCoordinator) n'a pas besoin d'attendre
     * cette future ; elle est surtout utile pour les tests et pour composer
     * d'autres futures (voir {@link #notifyFeedbackNegative}).
     */
    CompletableFuture<Void> notifyFeedbackReceived(Feedback feedback);

    /**
     * Notifie en plus chaque ADMIN lorsque l'avis soumis est jugé négatif
     * par l'analyse IA (Gemini) — en complément de
     * {@link #notifyFeedbackReceived}, destinée au manager. Même
     * comportement de parallélisation par destinataire.
     */
    CompletableFuture<Void> notifyFeedbackNegative(Feedback feedback);
}

