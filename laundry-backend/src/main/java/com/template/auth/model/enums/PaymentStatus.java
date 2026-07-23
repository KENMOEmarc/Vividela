package com.template.auth.model.enums;

/**
 * Enum representing the payment status of an order.
 * Values correspond to common payment lifecycle states.
 */
public enum PaymentStatus {
    PENDING,    // Payment is awaiting processing
    COMPLETED,  // Payment has been successfully completed
    FAILED,     // Payment processing failed
    REFUNDED    // Payment has been refunded to customer
}
