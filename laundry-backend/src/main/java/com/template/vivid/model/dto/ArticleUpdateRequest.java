package com.template.vivid.model.dto;

import com.template.vivid.model.enums.ArticleStatus;
import com.template.vivid.model.enums.FabricType;
import com.template.vivid.model.enums.ServiceType;
import com.template.vivid.model.enums.SizeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * DTO pour la mise à jour d'un article existant.
 * Tous les champs sont optionnels sauf le statut.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleUpdateRequest {

    private SizeType size;
    private FabricType fabric;

    @Size(max = 50, message = "La couleur ne peut pas dépasser 50 caractères")
    private String color;

    @Size(max = 255, message = "La distinction ne peut pas dépasser 255 caractères")
    private String distinction;

    @NotNull(message = "Le statut est requis")
    private ArticleStatus status;

    /**
     * Si fourni (non null), remplace intégralement les services de l'article
     * et déclenche le recalcul du prix appliqué / du total de la commande.
     * Si null, les services existants sont conservés tels quels.
     */
    private List<ServiceType> services;
}