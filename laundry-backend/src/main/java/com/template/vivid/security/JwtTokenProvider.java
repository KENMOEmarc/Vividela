package com.template.vivid.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * Fournisseur de tokens JWT (JSON Web Token).
 * <p>
 * STRUCTURE D'UN JWT :
 * Un JWT est une chaîne Base64URL encodée en 3 parties séparées par des points :
 * <p>
 * eyJhbGciOiJIUzI1NiJ9   ← Header  : algorithme de signature (HS256)
 * .
 * eyJzdWIiOiJ1c2VyQG...  ← Payload : claims (informations du token)
 * .
 * SflKxwRJSMeKKF2QT4fw   ← Signature : HMACSHA256(header + "." + payload, secret)
 * <p>
 * CLAIMS INCLUS DANS LE PAYLOAD :
 * sub  (subject)    : email de l'utilisateur
 * jti  (JWT ID)     : identifiant unique du token (pour la blacklist MySQL)
 * iat  (issued at)  : timestamp de création
 * exp  (expiration) : timestamp d'expiration
 * username          : claim personnalisé
 * userId            : claim personnalisé
 * <p>
 * SÉCURITÉ :
 * - Algorithme HS256 (HMAC-SHA256) avec clé symétrique 256 bits
 * - Le "secret" doit rester confidentiel côté serveur uniquement
 * - Un token signé ne peut pas être falsifié sans connaître le secret
 * - Le payload est visible (Base64 décodable) → ne jamais y mettre de données sensibles
 */
@Slf4j
@Component
public class JwtTokenProvider {

    // Aucune valeur secrète par défaut dans le code : le secret JWT doit être
    // fourni via la variable d'environnement JWT_SECRET.
    private static final int MIN_SECRET_LENGTH = 32; // 256 bits pour HS256

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Génère la clé HMAC à partir du secret.
     * <p>
     * BUGFIX : le secret configuré (jwt.secret) est une chaîne de caractères
     * en clair, PAS du Base64 (ex. valeur par défaut contenant "!!", qui n'est
     * pas un alphabet Base64 valide). L'ancienne implémentation appelait
     * Decoders.BASE64.decode(jwtSecret), ce qui levait systématiquement une
     * IllegalArgumentException ("Illegal base64 character") dès la première
     * génération/validation de token avec la config par défaut → authentification
     * cassée à 100% tant que JWT_SECRET n'était pas une valeur Base64 valide.
     * On utilise désormais directement les octets UTF-8 du secret, ce qui est
     * la pratique standard pour une clé HMAC fournie en texte brut.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Génère un token JWT pour un utilisateur authentifié.
     *
     * @param userId   ID de l'utilisateur en base
     * @param username Nom d'utilisateur
     * @param email    Email (= sujet du token)
     * @return Token JWT signé
     */
    public String generateToken(Long userId, String username, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                // Sujet du token = email (identifiant unique stable)
                .subject(email)
                // JWT ID unique → permet de blacklister ce token précis dans la table revoked_tokens
                .id(UUID.randomUUID().toString())
                // Claims personnalisés
                .claim("userId", userId)
                .claim("username", username)
                // Horodatages
                .issuedAt(now)
                .expiration(expiry)
                // Signature HMAC-SHA256
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extrait l'email (subject) du token.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extrait le JWT ID (jti) pour la gestion de la blacklist.
     */
    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    /**
     * Retourne la date d'expiration du token.
     */
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    /**
     * Durée restante avant expiration en millisecondes.
     * Utilisée pour définir l'expiration dans la table revoked_tokens lors de la blacklist.
     */
    public long getRemainingTtlMs(String token) {
        Date expiration = extractExpiration(token);
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    /**
     * Valide un token : signature valide + non expiré.
     *
     * @return true si valide, false sinon (les exceptions sont loggées)
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expiré: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Token JWT non supporté: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token JWT malformé: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Signature JWT invalide: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token JWT vide: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Durée de validité configurée (ms). Exposée pour la réponse API.
     */
    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Indique si le token est expiré (signature valide par ailleurs).
     * <p>
     * Utilisé par {@link JwtAuthenticationFilter} pour distinguer un token
     * expiré d'un token simplement invalide/malformé, afin que
     * l'entry point d'authentification puisse renvoyer un message 401
     * précis ("session expirée") au frontend, qui déclenche alors la
     * déconnexion automatique et la redirection vers /login.
     *
     * @return true si et seulement si le token est syntaxiquement/signature
     * valide mais que sa date d'expiration est dépassée.
     */
    public boolean isTokenExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse le token et retourne ses Claims (payload décodé et vérifié).
     * Lève une exception si la signature est invalide ou si le token est expiré.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}