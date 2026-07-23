package com.template.auth.service;

import com.template.auth.model.dto.AuthResponse;
import com.template.auth.model.dto.LoginRequest;
import com.template.auth.model.dto.RegisterRequest;

/**
 * Interface du service d'authentification.
 *
 * Orchestre les opérations d'inscription et de connexion en combinant :
 *   - UserService    (logique métier utilisateur)
 *   - JwtTokenProvider (génération/validation de tokens)
 *   - TokenBlacklistService (révocation de tokens via Redis)
 */
public interface AuthService {

    /**
     * Inscription + connexion automatique.
     *
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
     * @throws com.template.auth.exception.InvalidCredentialsException si identifiants invalides
     */
    AuthResponse login(LoginRequest request);

    /**
     * Déconnexion : révoque le token JWT courant dans Redis.
     *
     * Le token est ajouté à la blacklist Redis avec un TTL égal à
     * sa durée de vie restante. Ainsi, même si quelqu'un possède
     * un token volé, il ne peut plus l'utiliser après déconnexion.
     *
     * @param token Token JWT à révoquer (sans le préfixe "Bearer ")
     */
    void logout(String token);
}
