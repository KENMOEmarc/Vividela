package com.template.vivid.model.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StockMovementDto {
    private Long id;
    private Long stockId;
    private Long productId;
    private String productName;
    private BigDecimal quantity;
    private String movementType;
    private String notes;
    private Instant movementDate;
    private Long userId;
    private String userName;
}
