package com.vivid.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vivid.model.enums.RoleType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO complet pour la gestion des utilisateurs côté admin.
 *
 * Contient tous les champs nécessaires au dashboard :
 * id, identifiants, nom, statut, horodatages.
 * Le mot de passe n'est JAMAIS inclus dans cette réponse.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDto(
        Long id,
        String userName,
        String email,
        String phone,
        String firstName,
        String lastName,
        RoleType role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}