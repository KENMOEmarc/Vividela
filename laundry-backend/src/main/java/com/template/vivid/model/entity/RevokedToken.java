package com.template.vivid.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entité de stockage des tokens JWT révoqués (blacklist).
 *
 * REMPLACE REDIS :
 *   Cette table MySQL remplace la solution Redis pour gérer la blacklist des tokens JWT.
 *   Avantages :
 *   - Cohérence avec la même base de données que le reste de l'application
 *   - Audit/historique persistant des révocations
 *   - Pas de dépendance supplémentaire en terme de service déployé
 *
 * TTL SIMULÉ :
 *   Contrairement à Redis qui supprime automatiquement les clés expirées via TTL,
 *   ici on stocke expiresAt et on purge manuellement via un job planifié
 *   (RevokedTokenCleanupJob) qui s'exécute périodiquement.
 *
 * PERFORMANCE :
 *   - Index sur expiresAt pour accélérer les requêtes de purge
 *   - Index unique sur jti pour O(1) lookup lors de la vérification isBlacklisted()
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "revoked_tokens",
    indexes = {
        @Index(name = "idx_revoked_tokens_expires_at", columnList = "expires_at")
    }
)
public class RevokedToken {

    /**
     * Identifiant technique auto-généré.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * JWT ID unique du token (claim "jti" du JWT).
     * Clé fonctionnelle : permet de retrouver rapidement un token révoqué.
     */
    @Column(name = "jti", nullable = false, unique = true, length = 100)
    private String jti;

    /**
     * Date d'expiration du token (calcul : date_révocation + ttlMs).
     * Utilisée pour le nettoyage périodique : on supprime les lignes dont
     * expiresAt < now.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Date de révocation (pour audit/debug).
     */
    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

}

