package com.template.vivid.service.impl;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.template.vivid.model.enums.FeedbackSentiment;
import com.template.vivid.service.ReviewAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Implémentation de {@link ReviewAnalysisService} basée sur Spring AI et
 * l'API Gemini (module {@code spring-ai-starter-model-google-genai}, voir
 * pom.xml et application.yml — propriétés {@code spring.ai.google.genai.*}).
 * <p>
 * Le {@link ChatClient} est auto-configuré par Spring AI à partir de
 * {@code spring.ai.google.genai.api-key} / {@code spring.ai.google.genai.chat.model}
 * (positionné sur {@code gemini-2.5-flash} par défaut, voir application.yml) ;
 * il suffit d'injecter un {@link ChatClient.Builder}.
 * <p>
 * La sortie du modèle est directement désérialisée dans {@link GeminiReviewAnalysis}
 * grâce au support de sortie structurée de Spring AI ({@code .call().entity(...)}),
 * sans avoir à parser du JSON à la main.
 */
@Slf4j
@Service
public class GeminiReviewAnalysisService implements ReviewAnalysisService {

    private static final String SYSTEM_PROMPT = """
            Tu es un outil d'analyse d'avis clients pour un pressing (nettoyage à sec,
            blanchisserie) au Cameroun, nommé Vividela.
            
            Pour chaque avis reçu (une note de 1 à 5 et un commentaire optionnel),
            détermine le sentiment global du client parmi exactement trois valeurs :
              - POSITIVE : le client est satisfait ou très satisfait.
              - NEUTRAL  : avis mitigé, sans opinion tranchée, ou commentaire absent/insuffisant.
              - NEGATIVE : le client est insatisfait (délai non respecté, vêtement abîmé/perdu,
                mauvais accueil, tarif contesté, etc.).
            
            Base-toi en priorité sur le texte du commentaire ; utilise la note comme indice
            secondaire (une note basse avec un commentaire positif reste possible, et inversement).
            
            Fournis aussi un résumé très court (une phrase, en français, factuel, sans
            inventer de détails absents de l'avis) à destination du manager du pressing.
            """;

    private final ChatClient chatClient;

    // Même pool que celui utilisé pour l'envoi des notifications (voir
    // NotificationExecutorConfig) : l'appel Gemini est un I/O réseau, tout
    // comme l'envoi d'un email/SMS, donc partager le même pool dédié
    // (≥ 20 threads) est cohérent plutôt que de créer un exécuteur
    // supplémentaire pour un seul type d'appel.
    private final Executor notificationExecutor;

    public GeminiReviewAnalysisService(ChatClient.Builder chatClientBuilder,
                                       @Qualifier("notificationExecutor") Executor notificationExecutor) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
        this.notificationExecutor = notificationExecutor;
    }

    @Override
    public CompletableFuture<ReviewAnalysisResult> analyzeAsync(Integer rating, String comment) {
        return CompletableFuture.supplyAsync(() -> analyzeBlocking(rating, comment), notificationExecutor);
    }

    /**
     * Appel bloquant réel à Gemini — exécuté uniquement sur le pool
     * {@code notificationExecutor} via {@link #analyzeAsync}, jamais
     * directement sur un thread de requête HTTP.
     */
    private ReviewAnalysisResult analyzeBlocking(Integer rating, String comment) {
        String trimmedComment = comment != null ? comment.trim() : "";
        try {
            String userMessage = "Note du client (sur 5) : "
                    + (rating != null ? rating : "non renseignée")
                    + "\nCommentaire du client : "
                    + (trimmedComment.isEmpty() ? "(aucun commentaire laissé)" : "\"" + trimmedComment + "\"");

            GeminiReviewAnalysis analysis = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .entity(GeminiReviewAnalysis.class);

            if (analysis == null || analysis.sentiment() == null) {
                log.warn("Réponse Gemini vide ou incomplète lors de l'analyse d'un avis — repli sur l'heuristique par note.");
                return fallback(rating);
            }

            return new ReviewAnalysisResult(analysis.sentiment(), analysis.summary());
        } catch (Exception ex) {
            // Un incident sur l'appel IA (clé API absente/invalide, quota Gemini
            // dépassé, réseau indisponible…) ne doit jamais empêcher le client de
            // soumettre son avis : on retombe sur une classification simple basée
            // sur la note, et le manager reçoit tout de même la notification.
            log.error("Échec de l'analyse IA (Gemini) d'un avis client — repli sur l'heuristique par note.", ex);
            return fallback(rating);
        }
    }

    private ReviewAnalysisResult fallback(Integer rating) {
        FeedbackSentiment sentiment;
        if (rating == null) {
            sentiment = FeedbackSentiment.NEUTRAL;
        } else if (rating <= 2) {
            sentiment = FeedbackSentiment.NEGATIVE;
        } else if (rating == 3) {
            sentiment = FeedbackSentiment.NEUTRAL;
        } else {
            sentiment = FeedbackSentiment.POSITIVE;
        }
        return new ReviewAnalysisResult(sentiment, null);
    }

    @JsonClassDescription("Résultat de l'analyse de sentiment d'un avis client de pressing")
    private record GeminiReviewAnalysis(
            @JsonPropertyDescription("Sentiment global de l'avis : POSITIVE, NEUTRAL ou NEGATIVE")
            FeedbackSentiment sentiment,

            @JsonPropertyDescription("Résumé factuel en une phrase, en français, à destination du manager")
            String summary
    ) {
    }
}
