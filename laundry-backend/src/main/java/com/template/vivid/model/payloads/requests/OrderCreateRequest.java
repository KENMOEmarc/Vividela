package com.template.vivid.model.payloads.requests;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de création/mise à jour d'une commande.
 * Le client est résolu via userName (findByUserNameIgnoreCase en base).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    /** Username du client — résolu en User par OrderServiceImpl */
    private String userName;

    /**
     * Numéro de téléphone du client — utilisé en repli lorsque userName
     * n'est pas fourni ou ne correspond à personne (recherche fréquente
     * en boutique via le téléphone du client).
     */
    private String phone;

    private LocalDate expectedDeliveryDate;

    private BigDecimal discountAmount;

    private Integer loyaltyPointsUsed;

    private String shippingAddress;

    private String notes;
}