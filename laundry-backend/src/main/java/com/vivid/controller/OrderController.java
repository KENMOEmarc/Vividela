package com.vivid.controller;

import com.vivid.model.dto.*;
import com.vivid.model.dto.ArticleDto;
import com.vivid.model.dto.OrderDto;
import com.vivid.model.enums.OrderStatus;
import com.vivid.model.payloads.requests.*;
import com.vivid.model.payloads.requests.*;
import com.vivid.model.payloads.responses.ApiResponse;
import com.vivid.service.ArticleService;
import com.vivid.service.OrderService;
import com.vivid.service.UserService;
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
 * <p>
 * BUGFIX SÉCURITÉ : aucun de ces endpoints n'était protégé par @PreAuthorize.
 * Conséquence : N'IMPORTE QUEL utilisateur authentifié, y compris un simple
 * CUSTOMER, pouvait lister TOUTES les commandes de TOUS les clients, consulter
 * les commandes d'un client arbitraire, et modifier/supprimer n'importe quelle
 * commande. La gestion des commandes (création, consultation globale,
 * modification, suppression) est réservée au personnel (ADMIN / EMPLOYEE).
 * Les clients consultent leurs propres commandes via CustomerOrderController
 * (/customers/{id}/orders), qui vérifie désormais qu'ils accèdent bien à
 * leurs propres données.
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
     * <p>
     * AJOUT : filtrage côté serveur par statut pour l'écran "commandes en cours"
     * (cartes + checkboxs de filtre côté frontend).
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
     * AVANT qu'elle n'atteigne le stade READY (voir revue de code, règle
     * manquante n°10). updateOrder() ne permet cette transition qu'à partir
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

    /**
     * AJOUT : POST /orders/deposit — flux de dépôt atomique (commande +
     * articles en une seule transaction). Voir revue de code, règle
     * manquante n°17 (DTO DepositRequest jusque-là orphelin).
     */
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

    /**
     * AJOUT : POST /orders/search-by-barcode — retrouve une commande à
     * partir du code-barres imprimé sur son ticket, pour le guichet de
     * retrait. Voir revue de code, règle manquante n°15 (DTO BarcodeRequest
     * jusque-là orphelin).
     */
    @PostMapping("/search-by-barcode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDto>> findOrderByBarcode(@Valid @RequestBody BarcodeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Commande trouvée", orderService.findOrderByBarcode(request.getBarcode())));
    }

    // ── ORDER ITEMS (articles liés à une commande) ──────────────────────────

    /**
     * GET /orders/{id}/items  – récupère la commande avec ses articles.
     * Enveloppe dans ApiResponse pour la cohérence avec le reste de l'API.
     * <p>
     * CORRECTION : la réponse est maintenant wrappée dans ApiResponse<OrderDto>
     * au lieu de retourner OrderDto brut.
     */
    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderWithItems(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Commande avec articles récupérée",
                        orderService.getOrderById(id, true)));
    }

    /**
     * GET /orders/{orderId}/articles  – alias de /items pour compatibilité frontend.
     * <p>
     * CORRECTION BUG : le frontend appelait /orders/{id}/articles (méthode getOrderArticles)
     * mais le backend n'exposait que /orders/{id}/items → 404.
     * Cet alias résout le problème sans casser l'existant.
     * <p>
     * ÉVOLUTION : accessible aussi au CUSTOMER, mais uniquement pour consulter les
     * articles de SES PROPRES commandes (vérifié via clientUserId), afin d'afficher
     * le suivi des articles dans le dashboard client.
     */
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
        if (order.clientUserId() == null || !order.clientUserId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : vous ne pouvez consulter que vos propres commandes");
        }
    }

    /**
     * POST /orders/{orderId}/items  – ajoute un article à une commande.
     * <p>
     * CORRECTION : la réponse est maintenant wrappée dans ApiResponse<ArticleDto>.
     */
    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ArticleDto>> addOrderItem(
            @PathVariable Long orderId,
            @Valid @RequestBody ArticleCreateRequest request) {

        ArticleDto created = articleService.create(request, orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Article ajouté à la commande", created));
    }

    /**
     * PUT /orders/{orderId}/items/{itemId}  – met à jour un article d'une commande.
     * <p>
     * CORRECTION : la réponse est maintenant wrappée dans ApiResponse<ArticleDto>.
     */
    @PutMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ArticleDto>> updateOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody ArticleUpdateRequest request) {

        ArticleDto updated = articleService.updateArticle(itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Article mis à jour", updated));
    }

    /**
     * DELETE /orders/{orderId}/items/{itemId}  – supprime un article d'une commande.
     * <p>
     * CORRECTION : retourne 200 + ApiResponse au lieu de 204 vide,
     * pour être cohérent avec le reste de l'API.
     */
    @DeleteMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {

        articleService.deleteArticle(itemId);
        return ResponseEntity.ok(ApiResponse.success("Article supprimé de la commande", null));
    }

    // ── Utilitaire ──────────────────────────────────────────────────────────

    /**
     * Résout l'ID de l'utilisateur connecté depuis le contexte de sécurité.
     *
     * CORRECTION : la version précédente castait le principal en java.security.Principal
     * puis testait instanceof UserDetails — logique inversée qui causait un
     * ClassCastException. Spring Security injecte directement un UserDetails via
     * @AuthenticationPrincipal, ce qui est la bonne approche.
     *
     * @param userDetails  injecté par Spring Security via @AuthenticationPrincipal
     * @return ID de l'utilisateur connecté, ou null si non authentifié
     */
    /**
     * Résout l'ID de l'utilisateur connecté depuis le contexte de sécurité.
     * <p>
     * CORRECTION : UserDetailsServiceImpl retourne un Spring Security User
     * (org.springframework.security.core.userdetails.User), pas l'entité JPA.
     * On résout l'ID via le username (= email) en base de données.
     *
     * @param userDetails injecté par Spring Security via @AuthenticationPrincipal
     * @return ID de l'utilisateur connecté, ou null si non authentifié
     */
    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userService.findByEmail(userDetails.getUsername()).id();
    }
}