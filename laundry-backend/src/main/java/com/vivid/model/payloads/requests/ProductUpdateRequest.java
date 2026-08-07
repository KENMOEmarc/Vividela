package com.vivid.model.payloads.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating an existing Product
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Threshold value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Threshold value must be >= 0")
    private BigDecimal thresholdValue;

    @NotBlank(message = "Measurement unit is required")
    private String measurementUnit;
}

