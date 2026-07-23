package com.template.auth.service;

import com.template.auth.model.enums.FeedbackSentiment;

import java.util.concurrent.CompletableFuture;

/**
 * Analyse (via un modèle d'IA — Gemini 2.5, intégré avec Spring AI) le
 * sentiment d'un avis client laissé sur une commande livrée, avant que le
 * manager du pressing n'en soit notifié.
 * <p>
 * Voir GeminiReviewAnalysisService pour l'implémentation, et
 * FeedbackAnalysisCoordinator pour le point d'appel (pipeline asynchrone
 * déclenché après la soumission de l'avis — voir
 * FeedbackServiceImpl#submitFeedback).
 */
public interface ReviewAnalysisService {

    /**
     * Version asynchrone : l'appel réseau à Gemini (potentiellement lent,
     * de l'ordre de la seconde) s'exécute sur le pool dédié
     * {@code notificationExecutor} plutôt que sur le thread appelant, afin
     * de ne jamais bloquer un thread de la requête HTTP qui soumet l'avis.
     *
     * @param rating  note donnée par le client (1 à 5), peut être {@code null}
     * @param comment commentaire libre du client, peut être {@code null}/vide
     * @return une {@link CompletableFuture} qui se termine avec le sentiment
     *         détecté et un court résumé — ne se termine jamais en exception :
     *         en cas d'échec de l'appel au modèle (quota, réseau, clé API
     *         absente, réponse invalide…), l'implémentation doit résoudre la
     *         future avec un résultat de repli plutôt que de la faire échouer,
     *         pour ne jamais bloquer le traitement de l'avis du client.
     */
    CompletableFuture<ReviewAnalysisResult> analyzeAsync(Integer rating, String comment);

    /**
     * @param sentiment sentiment global de l'avis
     * @param summary   résumé en une phrase (français), {@code null} si non disponible
     */
    record ReviewAnalysisResult(FeedbackSentiment sentiment, String summary) {
    }
}
