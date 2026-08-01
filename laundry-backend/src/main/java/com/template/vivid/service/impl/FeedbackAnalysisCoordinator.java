package com.template.vivid.service.impl;

import com.template.vivid.model.entity.Feedback;
import com.template.vivid.model.enums.FeedbackSentiment;
import com.template.vivid.service.NotificationService;
import com.template.vivid.service.ReviewAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Orchestre, entièrement en asynchrone (chaîne de {@link CompletableFuture}
 * exécutée sur le pool {@code notificationExecutor}), le traitement d'un
 * avis client une fois qu'il a été soumis et persisté (note + commentaire) :
 *
 * <pre>
 *   1. analyse IA du commentaire (Gemini, via ReviewAnalysisService)
 *   2. persistance du sentiment + résumé sur le Feedback (FeedbackSentimentUpdater)
 *   3. notification du/des manager(s), et en plus de l'/des admin(s) si
 *      l'avis est jugé négatif (NotificationService)
 * </pre>
 * <p>
 * Déclenché par {@code FeedbackServiceImpl#submitFeedback}, une fois la
 * transaction de soumission committée (voir le commentaire à l'appel) — la
 * requête HTTP du client n'attend jamais la fin de cette chaîne : elle
 * répond dès que l'avis (note/commentaire) est enregistré, avec
 * {@code sentiment}/{@code aiSummary} encore {@code null} ("analyse en
 * cours"). Le client obtiendra ces champs en rechargeant le formulaire
 * (GET /orders/{id}/feedback).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackAnalysisCoordinator {

    private final ReviewAnalysisService reviewAnalysisService;
    private final FeedbackSentimentUpdater feedbackSentimentUpdater;
    private final NotificationService notificationService;

    // Unique bean de type Executor dans l'application (voir
    // NotificationExecutorConfig) : l'autowiring par type suffit, comme dans
    // NotificationServiceImpl.
    private final Executor notificationExecutor;

    /**
     * @return une {@link CompletableFuture} qui se termine une fois toute la
     *         chaîne (analyse + persistance + notifications) terminée —
     *         volontairement ignorée par l'appelant en production (l'envoi
     *         est "fire-and-forget" du point de vue de la requête HTTP),
     *         utile en revanche pour les tests.
     */
    public CompletableFuture<Void> processFeedbackAsync(Long feedbackId, Integer rating, String comment) {
        return reviewAnalysisService.analyzeAsync(rating, comment)
                // Appel à travers le proxy Spring du bean FeedbackSentimentUpdater
                // (pas d'auto-invocation) : @Transactional s'applique bien ici.
                .thenApplyAsync(result -> feedbackSentimentUpdater.applyAnalysis(feedbackId, result), notificationExecutor)
                .thenComposeAsync(this::dispatchNotifications, notificationExecutor)
                .exceptionally(ex -> {
                    log.error("Échec du traitement asynchrone de l'avis client (feedback {})", feedbackId, ex);
                    return null;
                });
    }

    private CompletableFuture<Void> dispatchNotifications(Feedback feedback) {
        if (feedback == null) {
            // Le feedback a disparu entre-temps (voir FeedbackSentimentUpdater) :
            // rien à notifier.
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> managerFuture = notificationService.notifyFeedbackReceived(feedback);
        if (feedback.getSentiment() == FeedbackSentiment.NEGATIVE) {
            CompletableFuture<Void> adminFuture = notificationService.notifyFeedbackNegative(feedback);
            return CompletableFuture.allOf(managerFuture, adminFuture);
        }
        return managerFuture;
    }
}
