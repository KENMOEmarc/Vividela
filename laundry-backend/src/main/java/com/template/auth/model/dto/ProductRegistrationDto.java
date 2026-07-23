package com.template.auth.model.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductRegistrationDto {
    private Long id;
    private Long productId;
    private String productName;
    private Long employeeUserId;
    private String employeeName;
    private BigDecimal quantity;
    private String registrationType;
    private String notes;
    private Instant registeredAt;
}
