package com.template.vivid.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.vivid.model.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Point d'entrée d'authentification — appelé par Spring Security chaque fois
 * qu'une requête non authentifiée (ou dont l'authentification a échoué)
 * atteint un endpoint protégé.
 * <p>
 * BUG CORRIGÉ : {@code SecurityConfig} ne déclarait aucun
 * {@link AuthenticationEntryPoint} personnalisé. Sans cette déclaration,
 * Spring Security retombe sur son comportement par défaut, qui ne garantit
 * PAS un code 401 cohérent (selon la configuration, un token expiré/absent
 * pouvait aboutir à un 403 "Forbidden" au lieu d'un 401 "Unauthorized").
 * Or le frontend (voir {@code authApi.js}, intercepteur de réponse Axios)
 * ne déclenche la déconnexion automatique + redirection vers /login QUE
 * sur un statut 401. Résultat : à l'expiration du token, l'utilisateur
 * restait "connecté" côté UI et recevait des erreurs 403 incompréhensibles
 * sur chaque action, sans jamais être renvoyé à l'écran de connexion.
 * <p>
 * En déclarant explicitement ce bean comme entryPoint (voir
 * {@code SecurityConfig#securityFilterChain}), toute requête non
 * authentifiée reçoit désormais toujours un 401 avec un corps JSON
 * {@link ApiResponse}, avec un message adapté selon la raison
 * (expiration, révocation, absence de token...) posée en attribut de
 * requête par {@link JwtAuthenticationFilter}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        String reason = (String) request.getAttribute("auth_error");
        String message = switch (reason == null ? "" : reason) {
            case "TOKEN_EXPIRED" -> "Votre session a expiré. Veuillez vous reconnecter.";
            case "TOKEN_REVOKED" -> "Votre session a été terminée. Veuillez vous reconnecter.";
            case "TOKEN_INVALID" -> "Session invalide. Veuillez vous reconnecter.";
            default -> "Authentification requise pour accéder à cette ressource.";
        };

        log.debug("Accès refusé sur {} : {}", request.getRequestURI(), message);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(message))
        );
    }
}
