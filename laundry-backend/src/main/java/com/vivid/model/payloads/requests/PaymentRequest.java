package com.vivid.model.payloads.requests;

import com.vivid.model.enums.PaymentMethodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotNull
    private Long orderId;

    @NotNull
    private PaymentMethodType paymentMethod;

    // AJOUT : montant strictement positif — voir revue de code, règle
    // manquante n°19 (un montant négatif ou nul passait la validation Bean
    // Validation et se retrouvait enregistré tel quel).
    @NotNull
    @DecimalMin(value = "0.01", message = "Le montant du paiement doit être strictement positif")
    private BigDecimal amount;

    private String payerPhone;

    private String transactionReference;
}

