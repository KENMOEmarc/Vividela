package com.vivid.model.dto;

import com.vivid.model.enums.ArticleStatus;
import com.vivid.model.enums.ClothingType;
import com.vivid.model.enums.FabricType;
import com.vivid.model.enums.SizeType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for reading/displaying Article information
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDto {

    private Long id;
    private Long orderId;
    private ClothingType clothingType;
    private SizeType size;
    private String sizeName;
    private FabricType fabric;
    /** Couleur en String — ex: "#FF5733" */
    private String color;
    private String distinction;
    private ArticleStatus status;
    /** Services de nettoyage appliqués à l'article, avec le prix tarifé pour chacun. */
    private List<ArticleServiceDto> services;
    /** Somme des prix appliqués (services) — coût de l'article dans la commande. */
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

