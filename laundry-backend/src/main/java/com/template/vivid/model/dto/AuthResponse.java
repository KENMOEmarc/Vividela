package com.template.vivid.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse après une authentification réussie (inscription ou connexion).
 *
 * CONTENU DE LA RÉPONSE :
 * {
 *   "accessToken": "eyJhbGci...",   ← Token JWT à stocker côté client
 *   "tokenType":   "Bearer",        ← Type d'authentification HTTP
 *   "expiresIn":   86400,           ← Durée de validité en secondes
 *   "user": {                        ← Informations non-sensibles de l'utilisateur
 *     "id":        1,
 *     "username":  "john_doe",
 *     "email":     "john@example.com",
 *     "phone":     "+1234567890",
 *     "firstName": "John",
 *     "lastName":  "Doe"
 *   }
 * }
 *
 * @JsonInclude(NON_NULL) : les champs null ne sont pas sérialisés en JSON
 * → réponse plus légère, pas de "field": null inutile.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    /** Token JWT signé. Le client doit l'envoyer dans chaque requête :
     *  Header: Authorization: Bearer <accessToken> */
    private String accessToken;

    /** Toujours "Bearer" selon la RFC 6750 (OAuth 2.0 Bearer Token). */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Durée de validité du token en secondes (ex: 86400 = 24h). */
    private Long expiresIn;

    /** Sous-objet avec les informations publiques de l'utilisateur. */
    private UserInfo user;

    /**
     * Informations de l'utilisateur retournées au client.
     * NE JAMAIS inclure le mot de passe (même haché) dans cette réponse.
     */
    @Getter
    @Setter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserInfo {
        private Long   id;
        private String userName;
        private String email;
        private String phone;
        private String firstName;
        private String lastName;
        private String role;
    }
}
