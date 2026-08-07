package com.vivid.model.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatsDto {
    private Integer totalOrders;
    private BigDecimal totalSpent;
}

