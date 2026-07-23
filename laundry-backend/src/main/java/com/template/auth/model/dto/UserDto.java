package com.template.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.template.auth.model.enums.RoleType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * DTO complet pour la gestion des utilisateurs côté admin.
 *
 * Contient tous les champs nécessaires au dashboard :
 * id, identifiants, nom, statut, horodatages.
 * Le mot de passe n'est JAMAIS inclus dans cette réponse.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private Long          id;
    private String        userName;
    private String        email;
    private String        phone;
    private String        firstName;
    private String        lastName;
    private RoleType role;
    private boolean       enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
