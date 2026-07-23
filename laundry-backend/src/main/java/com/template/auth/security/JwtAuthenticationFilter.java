package com.template.auth.security;

import com.template.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT — intercepte chaque requête HTTP pour valider le token.
 * <p>
 * CYCLE DE VIE D'UNE REQUÊTE :
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  1. Extraire le token de l'en-tête Authorization: Bearer <token>    │
 * │  2. Valider la signature et l'expiration                            │
 * │  3. Vérifier que le token n'est pas blacklisté (Redis)              │
 * │  4. Charger le UserDetails depuis la base de données                │
 * │  5. Injecter l'Authentication dans le SecurityContext               │
 * │  6. Passer la requête au filtre suivant (filterChain.doFilter)      │
 * └─────────────────────────────────────────────────────────────────────┘
 * <p>
 * OncePerRequestFilter : garantit que ce filtre n'est exécuté qu'une
 * seule fois par requête (évite les doubles vérifications en cas de
 * forward/include interne).
 * <p>
 * THREAD-SAFETY : SecurityContextHolder utilise un ThreadLocal → chaque
 * thread de requête a son propre contexte de sécurité isolé.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {

                // Vérifie si le token a été révoqué (déconnexion) via Redis
                if (tokenBlacklistService.isBlacklisted(jwtTokenProvider.extractJti(token))) {
                    log.debug("Token JWT blacklisté, accès refusé");
                    // Signale la raison précise à JwtAuthenticationEntryPoint pour que
                    // la réponse 401 renvoyée au frontend soit explicite (session
                    // terminée côté serveur → déconnexion + redirection /login).
                    request.setAttribute("auth_error", "TOKEN_REVOKED");
                    filterChain.doFilter(request, response);
                    return;
                }

                String email = jwtTokenProvider.extractEmail(token);

                // Évite de recharger le UserDetails si l'utilisateur est déjà authentifié
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // Crée le token d'authentification Spring Security
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,                        // credentials null (déjà authentifié via JWT)
                                    userDetails.getAuthorities()
                            );

                    // Enrichit avec les détails de la requête (IP, session…)
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Enregistre dans le SecurityContext → Spring Security sait que la requête est authentifiée
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Utilisateur '{}' authentifié via JWT", email);
                }
            } else if (token != null) {
                // Le token est présent mais invalide : on précise si c'est une
                // expiration (cas normal, l'utilisateur doit être reconnecté)
                // ou une autre invalidité (signature, format...), pour que
                // JwtAuthenticationEntryPoint renvoie un message 401 adapté
                // et que le frontend puisse déclencher la déconnexion
                // automatique + redirection vers /login.
                if (jwtTokenProvider.isTokenExpired(token)) {
                    request.setAttribute("auth_error", "TOKEN_EXPIRED");
                } else {
                    request.setAttribute("auth_error", "TOKEN_INVALID");
                }
            }

        } catch (Exception e) {
            // Ne pas bloquer la chaîne de filtres : si le token est invalide,
            // Spring Security refusera l'accès aux ressources protégées.
            log.error("Impossible d'authentifier l'utilisateur via JWT: {}", e.getMessage());
        }

        // Toujours passer la main au filtre suivant
        filterChain.doFilter(request, response);
    }

    /**
     * Extrait le token JWT de l'en-tête HTTP Authorization.
     * <p>
     * Format attendu : "Authorization: Bearer eyJhbGci..."
     * Retourne null si l'en-tête est absent ou mal formé.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
