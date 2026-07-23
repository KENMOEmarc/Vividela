package com.template.auth.model.dto;

import com.template.auth.model.enums.ServiceType;
import lombok.*;

import java.math.BigDecimal;

/**
 * Représente un service appliqué à un article, avec le prix tarifé
 * au moment de l'ajout (issu de ServicePrice).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleServiceDto {
    private ServiceType service;
    private BigDecimal appliedPrice;
}
