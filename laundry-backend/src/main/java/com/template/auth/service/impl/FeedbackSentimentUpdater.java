package com.template.auth.service.impl;

import com.template.auth.model.entity.Feedback;
import com.template.auth.repository.FeedbackRepository;
import com.template.auth.service.ReviewAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applique le résultat de l'analyse IA (Gemini) sur un {@link Feedback} déjà
 * soumis, dans sa propre transaction.
 * <p>
 * <b>Pourquoi un bean séparé plutôt qu'une méthode privée sur
 * {@code FeedbackAnalysisCoordinator}</b> : {@code applyAnalysis} est appelée
 * depuis un maillon d'une chaîne {@link java.util.concurrent.CompletableFuture}
 * (thread du pool {@code notificationExecutor}). Pour que {@code @Transactional}
 * s'applique réellement, l'appel doit obligatoirement passer par le proxy
 * Spring du bean — un appel {@code this.applyAnalysis(...)} depuis la même
 * classe (auto-invocation) contournerait ce proxy et l'annotation serait
 * silencieusement ignorée. D'où l'injection de ce bean, distinct, dans
 * {@code FeedbackAnalysisCoordinator}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackSentimentUpdater {

    private final FeedbackRepository feedbackRepository;

    /**
     * @return le {@link Feedback} mis à jour, avec {@code order} et
     *         {@code order.clientUser} déjà chargés (JOIN FETCH), prêt à être
     *         utilisé pour construire les notifications même après la fin de
     *         cette transaction (voir FeedbackRepository#findByIdWithOrderAndClient) ;
     *         {@code null} si le feedback a été supprimé entre-temps (cas
     *         limite, ne devrait pas arriver en pratique).
     */
    @Transactional
    public Feedback applyAnalysis(Long feedbackId, ReviewAnalysisService.ReviewAnalysisResult result) {
        return feedbackRepository.findByIdWithOrderAndClient(feedbackId)
                .map(feedback -> {
                    feedback.setSentiment(result.sentiment());
                    feedback.setAiSummary(result.summary());
                    return feedbackRepository.save(feedback);
                })
                .orElseGet(() -> {
                    log.warn("Feedback {} introuvable lors de l'application de l'analyse IA — abandon.", feedbackId);
                    return null;
                });
    }
}
