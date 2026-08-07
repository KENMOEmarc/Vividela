package com.vivid.controller;

import com.vivid.model.payloads.responses.ApiResponse;
import com.vivid.model.payloads.requests.ArticleCreateRequest;
import com.vivid.model.dto.ArticleDto;
import com.vivid.model.payloads.requests.ArticleUpdateRequest;
import com.vivid.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour les articles (vêtements d'une commande).
 * <p>
 * URL de base : /articles  (context-path=/api dans application.yml)
 * <p>
 * CORRECTIONS :
 * 1. Toutes les réponses sont maintenant enveloppées dans ApiResponse<T>
 * pour être cohérentes avec le reste de l'API.
 * (La version précédente retournait ArticleDto brut sur plusieurs endpoints.)
 * 2. Le frontend attend response.data.data pour accéder aux données
 * (ApiResponse.data contient le payload).
 * 3. BUGFIX SÉCURITÉ : aucun endpoint n'était protégé par @PreAuthorize —
 * n'importe quel utilisateur authentifié pouvait créer/modifier/supprimer
 * des articles. Restreint désormais au personnel (ADMIN / EMPLOYEE).
 */
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * POST /articles/order/{orderId}  – crée un article lié à une commande.
     * Réservé à ADMIN/MANAGER : un EMPLOYEE peut uniquement consulter et
     * modifier les articles existants (voir updateArticle), pas en créer.
     */
    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ArticleDto>> createArticle(
            @PathVariable Long orderId,
            @Valid @RequestBody ArticleCreateRequest request) {

        ArticleDto created = articleService.create(request, orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Article créé avec succès", created));
    }

    /**
     * POST /articles  – crée un article indépendant (sans commande associée).
     * Compatible avec le frontend qui poste sur /api/articles.
     * Réservé à ADMIN/MANAGER (même règle que createArticle ci-dessus).
     * <p>
     * CORRECTION : retourne ApiResponse<ArticleDto> au lieu de ArticleDto brut.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ArticleDto>> createArticleStandalone(
            @Valid @RequestBody ArticleCreateRequest request) {

        ArticleDto created = articleService.create(request, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Article créé avec succès", created));
    }

    /**
     * GET /articles/{id}  – récupère un article par son ID.
     * <p>
     * CORRECTION : retourne ApiResponse<ArticleDto> au lieu de ArticleDto brut.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ArticleDto>> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Article récupéré", articleService.getArticleById(id)));
    }

    /**
     * GET /articles  – liste tous les articles.
     * <p>
     * CORRECTION : retourne ApiResponse<List<ArticleDto>> au lieu de List<ArticleDto> brut.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ArticleDto>>> getAllArticles() {
        return ResponseEntity.ok(
                ApiResponse.success("Articles récupérés", articleService.getAllArticles()));
    }

    /**
     * PATCH /articles/{id}  – met à jour un article (mise à jour partielle,
     * pas de PUT : cohérent avec la même règle appliquée aux utilisateurs).
     * <p>
     * CORRECTION : retourne ApiResponse<ArticleDto> au lieu de ArticleDto brut.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ArticleDto>> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleUpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Article mis à jour", articleService.updateArticle(id, request)));
    }

    /**
     * DELETE /articles/{id}  – supprime un article.
     * <p>
     * CORRECTION : retourne 200 + ApiResponse<Void> au lieu de 204 vide,
     * pour être cohérent avec les autres endpoints de suppression de l'API.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.ok(ApiResponse.success("Article supprimé avec succès", null));
    }
}