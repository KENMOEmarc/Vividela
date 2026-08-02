package com.template.vivid.controller;

import com.template.vivid.model.dto.*;
import com.template.vivid.model.dto.CustomerStatsDto;
import com.template.vivid.service.OrderService;
import com.template.vivid.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contrôleur REST pour la gestion des clients et de leurs commandes.
 * <p>
 * URL de base : /customers  (context-path=/api est défini dans application.yml)
 * <p>
 * CORRECTION : suppression du préfixe "/api/" dans @RequestMapping.
 * L'ancienne valeur "/api/customers" produisait l'URL "/api/api/customers".
 * <p>
 * BUGFIX MAJEUR : le frontend (customerApi.js / CustomerFormModal / CustomersPage)
 * appelle déjà GET /customers/{id}, POST /customers, PUT /customers/{id},
 * DELETE /customers/{id} et GET /customers/{id}/stats — mais AUCUN de ces
 * endpoints n'existait côté backend (seul /customers/{id}/orders était exposé).
 * Conséquence : créer, modifier ou supprimer un client depuis le dashboard
 * admin échouait systématiquement avec une erreur 404. Ces endpoints sont
 * ajoutés ici, en délégant à UserService (un client est un User de rôle CUSTOMER).
 * <p>
 * BUGFIX SÉCURITÉ : GET /{customerId}/orders n'était protégé par aucune règle
 * d'autorisation — n'importe quel client connecté pouvait consulter les
 * commandes de n'importe quel autre client en changeant l'ID dans l'URL (IDOR).
 * On vérifie désormais que l'appelant est soit un membre du personnel
 * (ADMIN/EMPLOYEE), soit le client propriétaire des données demandées.
 * La création/modification/suppression de clients reste réservée au personnel.
 */
@RestController
@RequestMapping("/customers")   // ✅ CORRIGÉ : était "/api/customers"
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * GET /customers/{customerId}/orders
     * Retourne toutes les commandes d'un client donné.
     * Accessible au personnel, ou au client lui-même pour ses propres commandes.
     */
    @GetMapping("/{customerId}/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrdersByCustomer(
            @PathVariable Long customerId,
            @AuthenticationPrincipal UserDetails userDetails) {

        ensureStaffOrSelf(customerId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.success("Commandes du client récupérées",
                        orderService.getOrdersByClient(customerId)));
    }

    /**
     * GET /customers/{customerId}/stats
     * Statistiques d'un client : nombre de commandes + montant total dépensé.
     * Accessible au personnel, ou au client lui-même pour ses propres statistiques.
     */
    @GetMapping("/{customerId}/stats")
    public ResponseEntity<ApiResponse<CustomerStatsDto>> getCustomerStats(
            @PathVariable Long customerId,
            @AuthenticationPrincipal UserDetails userDetails) {

        ensureStaffOrSelf(customerId, userDetails);

        List<OrderDto> orders = orderService.getOrdersByClient(customerId);
        BigDecimal totalSpent = orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CustomerStatsDto stats = CustomerStatsDto.builder()
                .totalOrders(orders.size())
                .totalSpent(totalSpent)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Statistiques du client récupérées", stats));
    }

    /**
     * GET /customers/{id}
     * Récupère un client (User de rôle CUSTOMER) par son ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<UserDto>> getCustomerById(@PathVariable Long id) {
        UserDto user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Client récupéré", user));
    }

    /**
     * POST /customers
     * Crée un nouveau client depuis le dashboard admin.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<UserDto>> createCustomer(
            @Valid @RequestBody UserFormRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = userService.findByEmail(userDetails.getUsername()).getId();
        UserDto created = userService.createCustomer(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Client créé avec succès", created));
    }

    /**
     * PUT /customers/{id}
     * Met à jour les informations d'un client existant.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<UserDto>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UserFormRequest request) {

        UserDto updated = userService.updateCustomerInfo(id, request);
        return ResponseEntity.ok(ApiResponse.success("Client mis à jour avec succès", updated));
    }

    /**
     * DELETE /customers/{id}
     * Supprime un client.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = userService.findByEmail(userDetails.getUsername()).getId();
        userService.delete(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Client supprimé avec succès", null));
    }

    /**
     * Vérifie que l'appelant est un membre du personnel (ADMIN/EMPLOYEE)
     * ou le client lui-même (customerId == son propre ID).
     * Lève AccessDeniedException sinon (→ 403, géré par GlobalExceptionHandler).
     */
    private void ensureStaffOrSelf(Long customerId, UserDetails userDetails) {
        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_EMPLOYEE"));
        if (isStaff) {
            return;
        }
        UserDto current = userService.findByEmail(userDetails.getUsername());
        if (!current.getId().equals(customerId)) {
            throw new AccessDeniedException("Accès refusé : vous ne pouvez consulter que vos propres données");
        }
    }
}