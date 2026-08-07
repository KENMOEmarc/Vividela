package com.vivid.model.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record StockMovementDto(
        Long id,
        Long stockId,
        Long productId,
        String productName,
        BigDecimal quantity,
        String movementType,
        String notes,
        Instant movementDate,
        Long userId,
        String userName
) {
}