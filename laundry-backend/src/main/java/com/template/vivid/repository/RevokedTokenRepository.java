package com.template.vivid.repository;

import com.template.vivid.model.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    /**
     * Vérifie si un token est blacklisté via son JTI unique.
     * Génère un SELECT EXISTS → optimisé pour une simple vérification de présence.
     *
     * @param jti JWT ID du token à vérifier
     * @return true si le token est présent dans la blacklist, false sinon
     */
    boolean existsByJti(String jti);

    /**
     * Supprime tous les tokens révoqués dont la date d'expiration est dépassée.
     * Utilisée par le job planifié de purge (RevokedTokenCleanupJob).
     *
     * @param now Date actuelle (Instant.now())
     * @return Nombre de lignes supprimées
     */
    @Modifying
    @Query("DELETE FROM RevokedToken r WHERE r.expiresAt < :now")
    int deleteAllExpiredBefore(@Param("now") Instant now);
}

