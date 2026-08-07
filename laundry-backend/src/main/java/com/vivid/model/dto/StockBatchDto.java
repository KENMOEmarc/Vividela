package com.vivid.model.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Représente UN lot de stock précis (quantité, prix, date d'entrée, date
 * d'expiration). Un produit peut avoir plusieurs StockBatchDto.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StockBatchDto {
    private Long id;
    private Long productId;
    private String productName;
    private String measurementUnit;
    private BigDecimal currentQuantity;
    private BigDecimal unitPrice;
    private LocalDate entryDate;
    private LocalDate expirationDate;
    private boolean expired;
    private boolean expiringSoon;
    private Instant createdAt;
}
