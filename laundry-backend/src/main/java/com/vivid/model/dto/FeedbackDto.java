package com.vivid.model.dto;

import com.vivid.model.enums.FeedbackSentiment;
import lombok.Builder;

import java.time.Instant;

/**
 * Représentation publique du formulaire d'avis d'une commande. Ne contient
 * jamais l'entité Order/User complète (voir NotificationDto pour le
 * précédent de ce pattern).
 */
@Builder
public record FeedbackDto(
        Long id,
        Long orderId,
        Integer rating,
        String comment,
        FeedbackSentiment sentiment,
        String aiSummary,
        Instant requestedAt,
        Instant submittedAt,
        /** Raccourci pratique pour le frontend : {@code submittedAt != null}. */
        boolean submitted
) {
}