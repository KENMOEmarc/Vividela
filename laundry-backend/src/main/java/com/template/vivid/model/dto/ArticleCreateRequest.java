package com.template.vivid.model.dto;

import com.template.vivid.model.enums.ArticleStatus;
import com.template.vivid.model.enums.ClothingType;
import com.template.vivid.model.enums.FabricType;
import com.template.vivid.model.enums.ServiceType;
import com.template.vivid.model.enums.SizeType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * DTO pour la création d'un article (vêtement d'une commande de pressing).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCreateRequest {

    @NotNull(message = "Le type de vêtement est requis")
    private ClothingType clothingType;

    @NotNull(message = "La taille est requise")
    private SizeType size;

    @NotNull(message = "Le type de tissu est requis")
    private FabricType fabric;

    /** Couleur en texte libre ou code hex — ex: "#FF5733", "Rouge" */
    @Size(max = 50, message = "La couleur ne peut pas dépasser 50 caractères")
    private String color;

    @Size(max = 255, message = "La distinction ne peut pas dépasser 255 caractères")
    private String distinction;

    @NotNull(message = "Le statut est requis")
    private ArticleStatus status;

    /**
     * Services de nettoyage à appliquer à l'article (ex: WASH, IRON).
     * Détermine le prix appliqué (via ServicePrice) et donc le montant
     * de la commande.
     */
    @NotEmpty(message = "Sélectionnez au moins un service")
    private List<ServiceType> services;
}