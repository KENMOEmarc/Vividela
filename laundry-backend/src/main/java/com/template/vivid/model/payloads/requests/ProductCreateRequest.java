package com.template.vivid.model.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating a new Product
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Threshold value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Threshold value must be >= 0")
    private BigDecimal thresholdValue;

    @NotBlank(message = "Measurement unit is required")
    private String measurementUnit;
}

