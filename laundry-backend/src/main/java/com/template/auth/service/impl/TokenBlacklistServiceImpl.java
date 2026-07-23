package com.template.auth.service.impl;

import com.template.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Implémentation Redis de la blacklist de tokens JWT.
 *
 * PATTERN REDIS UTILISÉ : Simple Key-Value avec TTL
 *
 * Clé   : "jwt:blacklist:<jti>"
 * Valeur: "revoked"
 * TTL   : durée restante du token (Redis supprime automatiquement à l'expiration)
 *
 * AVANTAGE :
 *   - O(1) pour les opérations SET et EXISTS
 *   - Nettoyage automatique (TTL natif Redis)
 *   - Scalable : Redis peut gérer des millions de clés
 *   - Partagé entre plusieurs instances d'application (horizontal scaling)
 *
 * POURQUOI PAS UNE TABLE SQL ?
 *   - Une DB SQL nécessiterait un job de nettoyage périodique
 *   - Redis est plus rapide pour les lookups fréquents (chaque requête HTTP)
 *   - La durée de vie limitée correspond parfaitement au concept de TTL Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Override
    public void blacklist(String jti, long ttlMs) {
        try {
            String key = BLACKLIST_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(ttlMs));
            log.debug("Token ajouté à la blacklist Redis. Clé: {}, TTL: {}ms", key, ttlMs);
        } catch (Exception e) {
            // Redis indisponible : la révocation n'est pas persistée mais l'app reste fonctionnelle
            log.warn("Redis indisponible — révocation du token non persistée (jti: {}): {}", jti, e.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        try {
            String key = BLACKLIST_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            // Redis indisponible : on suppose que le token n'est pas révoqué
            log.warn("Redis indisponible — vérification blacklist ignorée (jti: {}): {}", jti, e.getMessage());
            return false;
        }
    }
}
