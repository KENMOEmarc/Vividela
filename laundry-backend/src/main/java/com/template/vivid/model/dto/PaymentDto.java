package com.template.vivid.model.dto;

import com.template.vivid.model.enums.PaymentMethodType;
import com.template.vivid.model.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private Long id;
    private Long orderId;
    private PaymentMethodType paymentMethod;
    private BigDecimal amount;
    private String payerPhone;
    private String transactionReference;
    private PaymentStatus status;
    private Instant paidAt;
    private Long createdBy;
}