package com.template.vivid.service.impl;

import com.template.vivid.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Job planifié de purge des tokens JWT révoqués expirés.
 * <p>
 * REMPLACE LE TTL REDIS :
 * Redis supprimait automatiquement les clés via TTL.
 * Avec MySQL, on doit supprimer manuellement les lignes dont expiresAt est dépassé.
 * <p>
 * FRÉQUENCE :
 * Par défaut toutes les heures (3600000 ms).
 * Configurable via la propriété security.jwt.blacklist-cleanup.fixed-delay-ms
 * dans application.yml.
 * <p>
 * PERFORMANCE :
 * - Suppression en masse (une seule requête DELETE) plutôt que par ligne
 * - Index sur expiresAt rend le filtrage rapide même avec une grande table
 * - Pas d'impact sur les performances du reste de l'application
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevokedTokenCleanupJob {

    private final RevokedTokenRepository revokedTokenRepository;

    /**
     * Purge périodique des tokens JWT révoqués dont la date d'expiration
     * est dépassée.
     *
     * @scheduled fixedDelayString : délai entre deux exécutions (ms)
     */
    @Scheduled(fixedDelayString = "${security.jwt.blacklist-cleanup.fixed-delay-ms:3600000}")
    @Transactional
    public void purgeExpiredRevokedTokens() {
        try {
            int deleted = revokedTokenRepository.deleteAllExpiredBefore(Instant.now());
            if (deleted > 0) {
                log.info("Purge blacklist JWT : {} token(s) expiré(s) supprimé(s)", deleted);
            } else {
                log.debug("Purge blacklist JWT : aucun token expiré à supprimer");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la purge des tokens révoqués expirés: {}", e.getMessage(), e);
        }
    }
}

