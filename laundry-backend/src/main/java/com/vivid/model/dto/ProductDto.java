package com.vivid.model.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for reading/displaying Product information
 */
@Builder
public record ProductDto(
        Long id,
        String name,
        BigDecimal thresholdValue,
        String measurementUnit,
        Instant createdAt,
        Instant updatedAt
) {
}