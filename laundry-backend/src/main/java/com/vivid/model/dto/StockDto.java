package com.vivid.model.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Vue AGRÉGÉE du stock d'un produit : somme des quantités de tous ses lots
 * (StockBatchDto). Le détail des lots peut être inclus via {@code batches}
 * lorsque l'on consulte le stock d'un produit précis.
 */
@Builder
public record StockDto(
        Long productId,
        String productName,
        String measurementUnit,

        /** Quantité totale, tous lots confondus. */
        BigDecimal currentQuantity,
        BigDecimal thresholdValue,
        boolean belowThreshold,

        /** Nombre de lots actifs pour ce produit. */
        int batchCount,

        /** Date d'expiration la plus proche parmi les lots restants (peut être null). */
        LocalDate nearestExpirationDate,

        /** Détail des lots (rempli uniquement pour la vue "produit précis"). */
        List<StockBatchDto> batches
) {
}