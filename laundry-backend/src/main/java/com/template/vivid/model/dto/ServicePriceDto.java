package com.template.vivid.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de lecture d'un tarif de service (prix appliqué pour un type de
 * vêtement + un type de service, ex: "Chemise" + "Nettoyage à sec").
 */
@Builder
public record ServicePriceDto(
        Long id,
        String clothingType,
        String service,
        BigDecimal price,
        Boolean active,
        Instant createdAt
) {
}