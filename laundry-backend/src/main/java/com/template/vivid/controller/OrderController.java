package com.template.vivid.controller;

import com.template.vivid.model.dto.*;
import com.template.vivid.model.enums.OrderStatus;
import com.template.vivid.model.payloads.requests.*;
import com.template.vivid.model.payloads.responses.ApiResponse;
import com.template.vivid.service.ArticleService;
import com.template.vivid.service.OrderService;
import com.template.vivid.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour les commandes.
 * <p>
 * URL de base : /orders  (le préfixe /api est défini dans application.yml via context-path)
 * <p>
 * CORRECTION : suppression du préfixe "/api/" dans @RequestMapping car
 * context-path=/api dans application.yml l'ajoute déjà automatiquement.
 * L'ancienne valeur "/api/orders" produisait l'URL "/api/api/orders" (double préfixe).
 */
@RestController
@RequestMapping("/orders")   // ✅ CORRIGÉ : était "/api/orders" → double préfixe avec context-path
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ArticleService articleService;
    private final UserService userService;

    // ── ORDERS ─────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = resolveUserId(userDetails);
        OrderDto created = orderService.createOrder(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Commande créée avec succès", created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeArticles) {

        return ResponseEntity.ok(
                ApiResponse.success("Commande récupérée",
                        orderService.getOrderById(id, includeArticles)));
    }

    /**
     * GET /orders  – liste les commandes, avec filtrage optionnel par statut(s).
     * <p>
     * Exemples :
     * GET /orders                                → toutes les commandes
     * GET /orders?status=PENDING                  → un seul statut
     * GET /orders?status=PENDING,IN_PROGRESS,READY → plusieurs statuts (checkboxs)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getAllOrders(
            @RequestParam(required = false) List<OrderStatus> status) {

        List<OrderDto> orders = (status == null || status.isEmpty())
                ? orderService.getAllOrders()
                : orderService.getOrdersByStatuses(status);

        return ResponseEntity.ok(ApiResponse.success("Commandes récupérées", orders));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrdersByClient(
            @PathVariable Long clientId) {

        return ResponseEntity.ok(
                ApiResponse.success("Commandes du client récupérées",
                        orderService.getOrdersByClient(clientId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = resolveUserId(userDetails);
        return ResponseEntity.ok(
                ApiResponse.success("Commande mise à jour avec succès",
                        orderService.updateOrder(id, request, currentUserId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Commande supprimée avec succès", null));
    }

    /**
     * AJOUT : PATCH /orders/{id}/cancel — annule une commande, y compris
     * AVANT qu'elle n'atteigne le stade READY. updateOrder() ne permet cette transition qu'à partir
     * de READY ; cette action dédiée couvre le cas le plus courant en
     * boutique (le client annule son dépôt).
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = resolveUserId(userDetails);
        return ResponseEntity.ok(
                ApiResponse.success("Commande annulée avec succès", orderService.cancelOrder(id, currentUserId)));
    }


    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDto>> createDeposit(
            @Valid @RequestBody DepositRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = resolveUserId(userDetails);
        OrderDto created = orderService.createDeposit(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dépôt enregistré avec succès", created));
    }


    @PostMapping("/search-by-barcode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDto>> findOrderByBarcode(@Valid @RequestBody BarcodeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Commande trouvée", orderService.findOrderByBarcode(request.getBarcode())));
    }

    // ── ORDER ITEMS (articles liés à une commande) ──────────────────────────

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderWithItems(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Commande avec articles récupérée",
                        orderService.getOrderById(id, true)));
    }

    @GetMapping("/{orderId}/articles")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderArticles(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        OrderDto order = orderService.getOrderById(orderId, true);
        ensureStaffOrOwner(order, userDetails);
        return ResponseEntity.ok(
                ApiResponse.success("Articles de la commande récupérés", order));
    }

    /**
     * Vérifie que l'appelant est membre du personnel, ou le client propriétaire
     * de la commande consultée. Lève AccessDeniedException (→ 403) sinon.
     */
    private void ensureStaffOrOwner(OrderDto order, UserDetails userDetails) {
        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_EMPLOYEE"));
        if (isStaff) {
            return;
        }
        Long currentUserId = resolveUserId(userDetails);
        if (order.getClientUserId() == null || !order.getClientUserId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : vous ne pouvez consulter que vos propres commandes");
        }
    }

    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ArticleDto>> addOrderItem(
            @PathVariable Long orderId,
            @Valid @RequestBody ArticleCreateRequest request) {

        ArticleDto created = articleService.create(request, orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Article ajouté à la commande", created));
    }

    @PutMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ArticleDto>> updateOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody ArticleUpdateRequest request) {

        ArticleDto updated = articleService.updateArticle(itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Article mis à jour", updated));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {

        articleService.deleteArticle(itemId);
        return ResponseEntity.ok(ApiResponse.success("Article supprimé de la commande", null));
    }

    /**
     * Résout l'ID de l'utilisateur connecté depuis le contexte de sécurité.
     * @param userDetails injecté par Spring Security via @AuthenticationPrincipal
     * @return ID de l'utilisateur connecté, ou null si non authentifié
     */
    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userService.findByEmail(userDetails.getUsername()).getId();
    }
}