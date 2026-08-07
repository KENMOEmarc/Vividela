package com.vivid.model.payloads.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête de connexion.
 *
 * L'utilisateur peut se connecter soit avec son email soit avec son username.
 * Le champ "identifier" accepte les deux — le service détecte lequel c'est
 * en vérifiant la présence du caractère '@'.
 *
 * SÉCURITÉ :
 *   - On ne révèle jamais si c'est l'email ou le mot de passe qui est incorrect
 *     (message générique "Identifiants invalides") → évite l'énumération de comptes.
 *   - Le mot de passe est reçu en clair (obligatoirement via HTTPS) puis
 *     comparé au hash BCrypt stocké en base.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    /**
     * Email ou nom d'utilisateur.
     */
    @NotBlank(message = "L'identifiant (email ou username) est obligatoire")
    private String identifier;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
