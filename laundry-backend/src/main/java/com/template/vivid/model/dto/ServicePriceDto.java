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
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePriceDto {

    private Long id;
    private String clothingType;
    private String service;
    private BigDecimal price;
    private Boolean active;
    private Instant createdAt;
}
