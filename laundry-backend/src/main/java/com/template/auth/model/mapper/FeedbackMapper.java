package com.template.auth.model.mapper;

import com.template.auth.model.dto.FeedbackDto;
import com.template.auth.model.entity.Feedback;

/**
 * Mapper centralisant la conversion Feedback (entity) → FeedbackDto.
 */
public final class FeedbackMapper {

    private FeedbackMapper() {
        // Classe utilitaire : pas d'instanciation
    }

    public static FeedbackDto toDto(Feedback feedback) {
        if (feedback == null) {
            return null;
        }

        return FeedbackDto.builder()
                .id(feedback.getId())
                .orderId(feedback.getOrder() != null ? feedback.getOrder().getId() : null)
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .sentiment(feedback.getSentiment())
                .aiSummary(feedback.getAiSummary())
                .requestedAt(feedback.getRequestedAt())
                .submittedAt(feedback.getSubmittedAt())
                .submitted(feedback.getSubmittedAt() != null)
                .build();
    }
}
