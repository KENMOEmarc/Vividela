package com.vivid.model.dto;

import com.vivid.model.enums.ArticleStatus;
import com.vivid.model.enums.ClothingType;
import com.vivid.model.enums.FabricType;
import com.vivid.model.enums.SizeType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for reading/displaying Article information
 */
@Builder
public record ArticleDto(
        Long id,
        Long orderId,
        ClothingType clothingType,
        SizeType size,
        String sizeName,
        FabricType fabric,
        /** Couleur en String — ex: "#FF5733" */
        String color,
        String distinction,
        ArticleStatus status,
        /** Services de nettoyage appliqués à l'article, avec le prix tarifé pour chacun. */
        List<ArticleServiceDto> services,
        /** Somme des prix appliqués (services) — coût de l'article dans la commande. */
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public ClothingType getClothingType() {
        return clothingType;
    }

    public SizeType getSize() {
        return size;
    }

    public String getColor() {
        return color;
    }

    public ArticleStatus getStatus() {
        return status;
    }

    public List<ArticleServiceDto> getServices() {
        return services;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}