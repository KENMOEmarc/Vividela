package com.template.vivid.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration CORS (Cross-Origin Resource Sharing).
 * PROBLÈME CORS :
 * Le navigateur bloque par défaut les requêtes vers un domaine différent
 * de celui de la page (politique Same-Origin). Notre frontend React tourne
 * sur http://localhost:5173 et le backend sur http://localhost:8080 →
 * origines différentes → CORS requis.
 * SOLUTION :
 * Le serveur inclut des en-têtes HTTP qui autorisent explicitement
 * les requêtes cross-origin depuis les origines whitelistées.
 * EN-TÊTES AJOUTÉS PAR SPRING :
 * Access-Control-Allow-Origin:  http://localhost:5173
 * Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
 * Access-Control-Allow-Headers: Authorization, Content-Type, ...
 * Access-Control-Allow-Credentials: true
 * Access-Control-Max-Age:       3600
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOriginsStr;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées (frontend dev + prod)
        config.setAllowedOriginPatterns(List.of(allowedOriginsStr.split(",")));

        // Méthodes HTTP autorisées
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // En-têtes autorisés dans les requêtes entrantes
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin"
        ));

        // Autorise l'envoi des cookies et de l'en-tête Authorization
        config.setAllowCredentials(true);

        // Durée de mise en cache de la réponse preflight OPTIONS (secondes)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Applique cette configuration CORS à toutes les routes
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
