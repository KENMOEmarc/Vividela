package com.vivid.model.payloads.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de création d'un tarif de service.
 *
 * Accessible aux ADMIN et MANAGER uniquement (voir ServicePriceController) :
 * ce sont les rôles habilités à définir les prix des services proposés.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePriceCreateRequest {

    @NotBlank(message = "Le type de vêtement est requis")
    private String clothingType;

    @NotBlank(message = "Le type de service est requis")
    private String service;

    @NotNull(message = "Le prix est requis")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être supérieur à 0")
    private BigDecimal price;

    private Boolean active;
}
