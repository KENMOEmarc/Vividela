package com.template.auth.model.mapper;

import com.template.auth.model.dto.ProductDto;
import com.template.auth.model.entity.Product;

/**
 * Mapper centralisant la conversion Product (entity) ↔ ProductDto.
 */
public final class ProductMapper {

    private ProductMapper() {
        // Classe utilitaire : pas d'instanciation
    }

    public static ProductDto toDto(Product product) {
        if (product == null) {
            return null;
        }
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .thresholdValue(product.getThresholdValue())
                .measurementUnit(String.valueOf(product.getMeasurementUnit()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
