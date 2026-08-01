package com.template.vivid.model.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Vue AGRÉGÉE du stock d'un produit : somme des quantités de tous ses lots
 * (StockBatchDto). Le détail des lots peut être inclus via {@code batches}
 * lorsque l'on consulte le stock d'un produit précis.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StockDto {
    private Long productId;
    private String productName;
    private String measurementUnit;

    /** Quantité totale, tous lots confondus. */
    private BigDecimal currentQuantity;
    private BigDecimal thresholdValue;
    private boolean belowThreshold;

    /** Nombre de lots actifs pour ce produit. */
    private int batchCount;

    /** Date d'expiration la plus proche parmi les lots restants (peut être null). */
    private LocalDate nearestExpirationDate;

    /** Détail des lots (rempli uniquement pour la vue "produit précis"). */
    private List<StockBatchDto> batches;
}
