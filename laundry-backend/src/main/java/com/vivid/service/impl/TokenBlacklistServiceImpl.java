package com.vivid.service.impl;

import com.vivid.model.entity.RevokedToken;
import com.vivid.repository.RevokedTokenRepository;
import com.vivid.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implémentation MySQL de la blacklist de tokens JWT.
 * <p>
 * REMPLACE REDIS :
 * Table revoked_tokens stocke les JTI des tokens révoqués.
 * Chaque ligne contient :
 * - jti : identifiant unique du token (clé de recherche rapide)
 * - expiresAt : date d'expiration du token
 * - revokedAt : timestamp de la révocation
 * <p>
 * TTL SIMULÉ :
 * Au lieu du TTL natif Redis, on supprime manuellement les lignes expirées
 * via un job planifié (RevokedTokenCleanupJob) qui s'exécute toutes les heures
 * (configurable via security.jwt.blacklist-cleanup.fixed-delay-ms).
 * GESTION ERREURS :
 * - DB indisponible : log warn, ne pas lever d'exception
 * - Double appel blacklist() avec même jti : géré en idempotence
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RevokedTokenRepository revokedTokenRepository;

    /**
     * Ajoute un token à la blacklist MySQL.
     * Gère l'idempotence : si le token est déjà révoqué, on log en debug et on sort.
     *
     * @param jti   JWT ID unique du token
     * @param ttlMs Durée de vie restante en millisecondes (pour calculer expiresAt)
     */
    @Override
    @Transactional
    public void blacklist(String jti, long ttlMs) {
        try {
            // Idempotence : vérifier que le token n'est pas déjà présent
            if (revokedTokenRepository.existsByJti(jti)) {
                log.debug("Token déjà blacklisté (jti: {})", jti);
                return;
            }

            Instant now = Instant.now();
            RevokedToken revokedToken = RevokedToken.builder()
                    .jti(jti)
                    .expiresAt(now.plusMillis(ttlMs))
                    .revokedAt(now)
                    .build();

            revokedTokenRepository.save(revokedToken);
            log.debug("Token ajouté à la blacklist MySQL (jti: {}, expiresAt dans {}ms)", jti, ttlMs);
        } catch (Exception e) {
            // DB indisponible : la révocation n'est pas persistée mais l'app reste fonctionnelle
            log.warn("Impossible de persister la révocation du token (jti: {}): {}", jti, e.getMessage());
        }
    }

    /**
     * Vérifie si un token est blacklisté.
     *
     * @param jti JWT ID du token
     * @return true si le token est présent dans la blacklist, false sinon
     */
    @Override
    public boolean isBlacklisted(String jti) {
        try {
            return revokedTokenRepository.existsByJti(jti);
        } catch (Exception e) {
            // DB indisponible : on suppose que le token n'est pas révoqué
            log.warn("Vérification blacklist impossible (jti: {}): {}", jti, e.getMessage());
            return false;
        }
    }
}
