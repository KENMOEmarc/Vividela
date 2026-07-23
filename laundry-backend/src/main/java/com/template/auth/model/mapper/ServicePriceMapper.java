package com.template.auth.model.mapper;

import com.template.auth.model.dto.ServicePriceDto;
import com.template.auth.model.entity.ServicePrice;

/**
 * Mapper centralisant la conversion ServicePrice (entity) ↔ ServicePriceDto.
 */
public final class ServicePriceMapper {

    private ServicePriceMapper() {
        // Classe utilitaire : pas d'instanciation
    }

    public static ServicePriceDto toDto(ServicePrice servicePrice) {
        if (servicePrice == null) {
            return null;
        }
        return ServicePriceDto.builder()
                .id(servicePrice.getId())
                .clothingType(String.valueOf(servicePrice.getClothingType()))
                .service(String.valueOf(servicePrice.getService()))
                .price(servicePrice.getPrice())
                .active(servicePrice.getActive())
                .createdAt(servicePrice.getCreatedAt())
                .build();
    }
}
