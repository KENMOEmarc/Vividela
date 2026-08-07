package com.template.vivid.model.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CustomerStatsDto(
        Integer totalOrders,
        BigDecimal totalSpent
) {
}