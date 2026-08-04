package com.template.vivid.controller;

import com.template.vivid.model.payloads.responses.ApiResponse;
import com.template.vivid.model.payloads.requests.ArticleCreateRequest;
import com.template.vivid.model.dto.ArticleDto;
import com.template.vivid.model.payloads.requests.ArticleUpdateRequest;
import com.template.vivid.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ArticleDto>> createArticle(
            @PathVariable Long orderId,
            @Valid @RequestBody ArticleCreateRequest request) {

        ArticleDto created = articleService.create(request, orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Article créé avec succès", created));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ArticleDto>> createArticleStandalone(
            @Valid @RequestBody ArticleCreateRequest request) {

        ArticleDto created = articleService.create(request, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Article créé avec succès", created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ArticleDto>> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Article récupéré", articleService.getArticleById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ArticleDto>>> getAllArticles() {
        return ResponseEntity.ok(
                ApiResponse.success("Articles récupérés", articleService.getAllArticles()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ArticleDto>> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleUpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Article mis à jour", articleService.updateArticle(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.ok(ApiResponse.success("Article supprimé avec succès", null));
    }
}