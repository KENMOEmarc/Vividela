package com.template.auth.model.dto;

import com.template.auth.model.enums.FeedbackSentiment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Représentation publique du formulaire d'avis d'une commande. Ne contient
 * jamais l'entité Order/User complète (voir NotificationDto pour le
 * précédent de ce pattern).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackDto {
    private Long id;
    private Long orderId;
    private Integer rating;
    private String comment;
    private FeedbackSentiment sentiment;
    private String aiSummary;
    private Instant requestedAt;
    private Instant submittedAt;
    /** Raccourci pratique pour le frontend : {@code submittedAt != null}. */
    private boolean submitted;
}
