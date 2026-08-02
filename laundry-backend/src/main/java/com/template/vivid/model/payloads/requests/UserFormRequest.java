package com.template.vivid.model.payloads.requests;

import com.template.vivid.model.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO pour la création et la modification d'un utilisateur depuis le dashboard admin.
 *
 * RÈGLES DE VALIDATION :
 *   - username, email, firstName, lastName : toujours requis
 *   - password : requis à la création, optionnel en mise à jour
 *     (null ou vide = conserver le mot de passe existant)
 *   - enabled  : état du compte (actif / désactivé)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFormRequest {

    @NotBlank(message = "Le nom d'utilisateur est requis")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit faire entre 3 et 50 caractères")
    private String userName;

    @NotBlank(message = "L'adresse email est requise")
    @Email(message = "Format d'email invalide")
    private String email;

    @Size(max = 20)
    @NotBlank(message = "Le numéro de téléphone est requis")
    private String phone;

    @NotBlank(message = "Le prénom est requis")
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String lastName;

    /** Optionnel pour la mise à jour ; requis pour la création (validé dans le service). */
    @Size(min = 8, max = 100, message = "Le mot de passe doit faire au moins 8 caractères")
    private String password;

    /** Rôle attribué à l'utilisateur. Par défaut CUSTOMER. */
    private RoleType role = RoleType.CUSTOMER;

    private boolean enabled = true;

    private Long userId;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();
}
