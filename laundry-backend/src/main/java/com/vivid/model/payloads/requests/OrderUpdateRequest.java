package com.vivid.model.payloads.requests;

import com.vivid.model.enums.OrderStatus;
import com.vivid.model.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateRequest {
    /**
     * Username du client — résolu en User par OrderServiceImpl
     */
    private String userName;

    /**
     * Numéro de téléphone du client — utilisé en repli lorsque userName
     * n'est pas fourni ou ne correspond à personne (recherche fréquente
     * en boutique via le téléphone du client).
     */
    private String phone;

    private LocalDate depositDate;

    private LocalDate expectedDeliveryDate;

    private OrderStatus status;

    private PaymentStatus paymentStatus;

    private BigDecimal discountAmount;

    private Integer loyaltyPointsUsed;

    private String shippingAddress;

    private String notes;
}
