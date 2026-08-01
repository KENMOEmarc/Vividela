package com.template.vivid.service;

/**
 * Service de blacklist des tokens JWT révoqués.
 *
 * PROBLÈME :
 *   Les tokens JWT sont stateless : une fois émis, ils sont valides jusqu'à
 *   leur expiration. On ne peut pas les "invalider" côté serveur par défaut.
 *
 * SOLUTION — Blacklist MySQL :
 *   À la déconnexion, on stocke le JTI (JWT ID) du token dans la table
 *   revoked_tokens avec une date d'expiration (expiresAt). Ainsi :
 *   - Chaque requête vérifie si le JTI est présent dans la table
 *   - Si oui → token révoqué → accès refusé
 *   - Un job planifié supprime les lignes dont expiresAt est dépassé
 *     (nettoyage périodique au lieu du TTL natif Redis)
 *
 * FORMAT TABLE MYSQL :
 *   TABLE revoked_tokens :
 *     - jti (VARCHAR 100, UNIQUE)       → JWT ID du token
 *     - expiresAt (DATETIME)             → date d'expiration du token
 *     - revokedAt (DATETIME)             → timestamp de révocation
 *   Index : idx_revoked_tokens_expires_at sur expiresAt (pour la purge)
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
