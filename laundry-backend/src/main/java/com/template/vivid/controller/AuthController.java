package com.template.vivid.controller;

import com.template.vivid.model.payloads.responses.ApiResponse;
import com.template.vivid.model.payloads.responses.AuthResponse;
import com.template.vivid.model.payloads.requests.LoginRequest;
import com.template.vivid.model.payloads.requests.RegisterRequest;
import com.template.vivid.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour l'authentification.
 * <p>
 * URL DE BASE : /api/auth  (préfixe /api défini dans application.yml)
 * <p>
 * ROUTES EXPOSÉES :
 * ┌────────────────────────────────────────────────────────────────────┐
 * │  POST /api/auth/register  → Inscription d'un nouvel utilisateur   │
 * │  POST /api/auth/login     → Connexion et obtention du token JWT   │
 * │  POST /api/auth/logout    → Révocation du token courant           │
 * └────────────────────────────────────────────────────────────────────┘
 * <p>
 * RÔLE DU CONTRÔLEUR (MVC — couche C) :
 * - Reçoit et désérialise le JSON entrant (@RequestBody)
 * - Déclenche la validation (@Valid → annotations Jakarta)
 * - Délègue la logique métier au Service (jamais de logique ici)
 * - Construit et retourne la ResponseEntity (status HTTP + corps JSON)
 *
 * @RestController = @Controller + @ResponseBody
 * → chaque méthode retourne directement du JSON (pas de vue Thymeleaf/JSP)
 * @RequestMapping → préfixe d'URL commun à toutes les routes du contrôleur
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * <p>
     * Inscrit un nouvel utilisateur et retourne un token JWT.
     *
     * @return 201 Created + token JWT si succès
     * @Valid déclenche la validation du DTO :
     * - Champs obligatoires (@NotBlank)
     * - Format email (@Email)
     * - Taille (@Size)
     * - Regex (@Pattern)
     * En cas d'erreur → GlobalExceptionHandler intercepte et retourne 400.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.debug("Requête d'inscription reçue pour: {}", request.getEmail());

        AuthResponse authResponse = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)          // 201 Created (ressource créée)
                .body(ApiResponse.success(
                        "Inscription réussie ! Bienvenue " + authResponse.getUser().getFirstName(),
                        authResponse
                ));
    }

    /**
     * POST /api/auth/login
     * <p>
     * Authentifie un utilisateur existant.
     *
     * @return 200 OK + token JWT si succès
     * 401 Unauthorized si identifiants invalides (géré par GlobalExceptionHandler)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.debug("Requête de connexion reçue pour: {}", request.getIdentifier());

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success("Connexion réussie", authResponse)
        );
    }

    /**
     * POST /api/auth/logout
     * <p>
     * Révoque le token JWT courant en l'ajoutant à la blacklist Redis.
     * Requiert un token valide dans le header Authorization.
     * <p>
     * Le token est extrait de l'en-tête Authorization: Bearer <token>
     * et transmis à AuthService.logout() pour révocation.
     *
     * @return 200 OK si token révoqué ou absent (idempotent)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Extrait le token brut (sans le préfixe "Bearer ")
        String token = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        authService.logout(token);

        return ResponseEntity.ok(
                ApiResponse.success("Déconnexion réussie")
        );
    }
}
