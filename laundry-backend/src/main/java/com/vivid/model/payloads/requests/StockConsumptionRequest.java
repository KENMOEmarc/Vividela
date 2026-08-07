package com.vivid.model.payloads.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Requête de consommation de stock pour un produit. La quantité est
 * décomptée automatiquement sur les lots existants en suivant la stratégie
 * FEFO (First Expired, First Out) : le lot qui expire le plus tôt est
 * consommé en premier.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StockConsumptionRequest {

    @NotNull(message = "L'ID produit est obligatoire")
    private Long productId;

    @NotNull(message = "La quantité est obligatoire")
    @DecimalMin(value = "0.01", message = "La quantité doit être > 0")
    private BigDecimal quantity;

    private String notes;
}
