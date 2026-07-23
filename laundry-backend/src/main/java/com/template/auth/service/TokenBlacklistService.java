package com.template.auth.service;

/**
 * Service de blacklist des tokens JWT révoqués.
 *
 * PROBLÈME :
 *   Les tokens JWT sont stateless : une fois émis, ils sont valides jusqu'à
 *   leur expiration. On ne peut pas les "invalider" côté serveur par défaut.
 *
 * SOLUTION — Blacklist Redis :
 *   À la déconnexion, on stocke le JTI (JWT ID) du token dans Redis avec
 *   un TTL égal à la durée de vie restante du token. Ainsi :
 *   - Chaque requête vérifie si le JTI est présent dans Redis
 *   - Si oui → token révoqué → accès refusé
 *   - Redis supprime automatiquement la clé à l'expiration du token
 *     (pas besoin de nettoyage manuel, TTL géré par Redis)
 *
 * FORMAT CLÉ REDIS :
 *   "jwt:blacklist:<jti>"  → valeur "revoked"
 *   TTL = millisecondes restantes avant expiration du token
 */
public interface TokenBlacklistService {

    /**
     * Ajoute un token à la blacklist Redis.
     *
     * @param jti     JWT ID unique du token (claim "jti")
     * @param ttlMs   Durée de vie restante en millisecondes (TTL Redis)
     */
    void blacklist(String jti, long ttlMs);

    /**
     * Vérifie si un token est blacklisté.
     *
     * @param jti JWT ID du token à vérifier
     * @return true si le token est révoqué
     */
    boolean isBlacklisted(String jti);
}
