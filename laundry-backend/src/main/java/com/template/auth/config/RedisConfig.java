package com.template.auth.config;



import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuration Redis.
 *
 * RÔLES DE REDIS DANS CE PROJET :
 * ┌────────────────────────────────────────────────────────────────┐
 * │  1. BLACKLIST JWT                                              │
 * │     Clé   : "blacklist:<jti>"  (JTI = JWT ID unique)         │
 * │     Valeur: "revoked"                                          │
 * │     TTL   : durée restante du token                           │
 * │     → Permet d'invalider un token avant son expiration        │
 * │       (déconnexion, changement de mot de passe)               │
 * │                                                                │
 * │  2. CACHE APPLICATIF (via @Cacheable)                         │
 * │     Clé   : nom du cache + paramètres de la méthode          │
 * │     Valeur: résultat sérialisé en JSON                        │
 * │     TTL   : 1 heure par défaut                               │
 * └────────────────────────────────────────────────────────────────┘
 *
 * SÉRIALISATION :
 *   - Clés     → StringRedisSerializer (lisible en console Redis)
 *   - Valeurs  → GenericJackson2JsonRedisSerializer (JSON compact)
 */
@Configuration
public class RedisConfig {

    /**
     * Template générique pour les opérations Redis manuelles.
     *
     * Utilisé dans TokenBlacklistService pour set/get/exists.
     * La sérialisation String/JSON évite les problèmes de caractères
     * non imprimables du sérialiseur Java par défaut.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer();

        // Sérialiseur pour les clés (toujours String)
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Sérialiseur pour les valeurs (JSON)
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Gestionnaire de cache Spring Cache (abstraction haut niveau).
     *
     * Permet d'utiliser @Cacheable / @CacheEvict / @CachePut
     * sur les méthodes de service sans écrire du code Redis manuellement.
     *
     * Configuration par défaut appliquée à tous les caches :
     *   - TTL = 1 heure
     *   - Sérialisation JSON des valeurs
     *   - Pas de mise en cache des valeurs null
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            );

        return (CacheManager) RedisCacheManager .builder(factory)
            .cacheDefaults(defaultConfig)
            .build();
    }
}
