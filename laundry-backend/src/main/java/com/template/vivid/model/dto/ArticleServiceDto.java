package com.vivid.model.dto;

import com.vivid.model.enums.ServiceType;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Représente un service appliqué à un article, avec le prix tarifé
 * au moment de l'ajout (issu de ServicePrice).
 */
@Builder
public record ArticleServiceDto(
        ServiceType service,
        BigDecimal appliedPrice
) {

}