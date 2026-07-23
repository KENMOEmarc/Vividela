package com.template.auth.model.enums;

/**
 * Sentiment détecté par l'analyse IA (Gemini, via Spring AI) d'un avis
 * client laissé sur une commande livrée. Voir Feedback / FeedbackService.
 */
public enum FeedbackSentiment {
    POSITIVE,
    NEUTRAL,
    NEGATIVE
}
