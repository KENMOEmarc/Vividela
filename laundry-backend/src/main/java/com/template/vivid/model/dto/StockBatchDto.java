package com.template.vivid.model.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Représente UN lot de stock précis (quantité, prix, date d'entrée, date
 * d'expiration). Un produit peut avoir plusieurs StockBatchDto.
 */
@Builder
public record StockBatchDto(
        Long id,
        Long productId,
        String productName,
        String measurementUnit,
        BigDecimal currentQuantity,
        BigDecimal unitPrice,
        LocalDate entryDate,
        LocalDate expirationDate,
        boolean expired,
        boolean expiringSoon,
        Instant createdAt
) {
}