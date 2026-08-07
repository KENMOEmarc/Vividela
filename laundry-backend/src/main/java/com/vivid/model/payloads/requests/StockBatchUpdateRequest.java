package com.vivid.model.payloads.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête de correction manuelle d'un lot de stock existant
 * (quantité, prix, dates). Génère un mouvement de type ADJUSTMENT.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StockBatchUpdateRequest {

    @NotNull(message = "La quantité est obligatoire")
    @DecimalMin(value = "0.00", message = "La quantité doit être ≥ 0")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "Le prix doit être positif")
    private BigDecimal unitPrice;

    private LocalDate entryDate;

    private LocalDate expirationDate;

    private String notes;
}
