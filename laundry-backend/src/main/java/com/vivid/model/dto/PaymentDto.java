package com.vivid.model.dto;

import com.vivid.model.enums.PaymentMethodType;
import com.vivid.model.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record PaymentDto(
        Long id,
        Long orderId,
        PaymentMethodType paymentMethod,
        BigDecimal amount,
        String payerPhone,
        String transactionReference,
        PaymentStatus status,
        Instant paidAt,
        Long createdBy
) {
}