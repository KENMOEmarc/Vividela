package com.vivid.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for reading/displaying Product information
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long id;
    private String name;
    private BigDecimal thresholdValue;
    private String measurementUnit;
    private Instant createdAt = Instant.now();
    private Instant updatedAt;
}

