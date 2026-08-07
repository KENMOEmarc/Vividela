package com.vivid.model.payloads.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête de création d'un nouveau lot de stock (réapprovisionnement).
 * Chaque lot possède sa propre quantité, son propre prix d'achat, sa date
 * d'entrée en stock et, si applicable, sa date d'expiration.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StockBatchCreateRequest {

    @NotNull(message = "L'ID produit est obligatoire")
    private Long productId;

    @NotNull(message = "La quantité est obligatoire")
    @DecimalMin(value = "0.01", message = "La quantité doit être > 0")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "Le prix doit être positif")
    private BigDecimal unitPrice;

    /** Date d'entrée en stock. Si non fournie, la date du jour est utilisée. */
    private LocalDate entryDate;

    /** Date d'expiration du lot. Facultative (produits non périssables). */
    private LocalDate expirationDate;

    private String notes;
}
