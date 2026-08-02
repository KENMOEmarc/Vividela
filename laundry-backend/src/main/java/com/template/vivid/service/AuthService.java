package com.template.vivid.service;

import com.template.vivid.model.payloads.responses.AuthResponse;
import com.template.vivid.model.payloads.requests.LoginRequest;
import com.template.vivid.model.payloads.requests.RegisterRequest;

/**
 * Interface du service d'authentification.
 * <p>
 * Orchestre les opérations d'inscription et de connexion en combinant :
 * - UserService    (logique métier utilisateur)
 * - JwtTokenProvider (génération/validation de tokens)
 * - TokenBlacklistService (révocation de tokens via table MySQL revoked_tokens)
 */
public interface AuthService {

    /**
     * Inscription + connexion automatique.
     * <p>
     * Crée l'utilisateur ET génère immédiatement un token JWT
     * pour éviter à l'utilisateur de se reconnecter après l'inscription.
     *
     * @param request Données d'inscription (username, email, password…)
     * @return Réponse avec token JWT + informations de l'utilisateur
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Connexion avec email/username + mot de passe.
     *
     * @param request Identifiant + mot de passe
     * @return Réponse avec token JWT + informations de l'utilisateur
     * @throws com.template.vivid.exception.InvalidCredentialsException si identifiants invalides
     */
    AuthResponse login(LoginRequest request);

    /**
     * Déconnexion : révoque le token JWT courant dans la table revoked_tokens.
     * <p>
     * Le token est ajouté à la blacklist avec une date d'expiration égale à
     * sa durée de vie restante. Ainsi, même si quelqu'un possède
     * un token volé, il ne peut plus l'utiliser après déconnexion.
     *
     * @param token Token JWT à révoquer (sans le préfixe "Bearer ")
     */
    void logout(String token);
}
