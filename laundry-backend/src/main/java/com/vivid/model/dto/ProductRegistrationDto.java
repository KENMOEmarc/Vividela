package com.vivid.model.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record ProductRegistrationDto(
        Long id,
        Long productId,
        String productName,
        Long employeeUserId,
        String employeeName,
        BigDecimal quantity,
        String registrationType,
        String notes,
        Instant registeredAt
) {

}