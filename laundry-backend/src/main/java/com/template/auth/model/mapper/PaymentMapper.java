package com.template.auth.model.mapper;

import com.template.auth.model.dto.PaymentDto;
import com.template.auth.model.entity.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static PaymentDto toDto(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentDto.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .payerPhone(payment.getPayerPhone())
                .transactionReference(payment.getTransactionReference())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdBy(payment.getCreatedBy() != null ? payment.getCreatedBy().getId() : null)
                .build();
    }
}

