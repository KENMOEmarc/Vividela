package com.template.auth.controller;

import com.template.auth.model.dto.ApiResponse;
import com.template.auth.model.dto.AuthResponse;
import com.template.auth.model.dto.UserDto;
import com.template.auth.model.dto.UserFormRequest;
import com.template.auth.model.entity.User;
import com.template.auth.model.mapper.UserMapper;
import com.template.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrôleur REST pour les opérations sur les utilisateurs.
 * <p>
 * URL DE BASE : /api/users
 * <p>
 * ROUTES EXPOSÉES (toutes nécessitent un JWT valide) :
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  GET    /api/users        → Liste de tous les utilisateurs          │
 * │                              (ADMIN, MANAGER, EMPLOYEE)              │
 * │  POST   /api/users        → Créer un utilisateur (ADMIN, MANAGER)   │
 * │  GET    /api/users/me     → Profil de l'utilisateur connecté        │
 * │  GET    /api/users/{id}   → Récupérer un utilisateur par ID         │
 * │  PATCH  /api/users/{id}   → Mettre à jour un utilisateur            │
 * │                              (ADMIN, MANAGER, EMPLOYEE — sous       │
 * │                              réserve des règles de hiérarchie de    │
 * │                              rôles et du mot de passe, voir         │
 * │                              UserServiceImpl)                       │
 * │  DELETE /api/users/{id}   → Supprimer un utilisateur (ADMIN)        │
 * └──────────────────────────────────────────────────────────────────────┘
 * <p>
 * RÈGLES MÉTIER (voir UserServiceImpl pour le détail) :
 *   - Un MANAGER ou un EMPLOYEE ne peut ni consulter-modifier (PATCH) un
 *     compte ADMIN, ni s'attribuer/attribuer à un tiers un rôle supérieur
 *     au sien.
 *   - Un EMPLOYEE peut uniquement consulter et modifier les utilisateurs
 *     (pas de création, pas de suppression).
 *   - Seul un ADMIN peut changer le mot de passe d'un compte existant.
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── Profil personnel ────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Récupération du profil pour: {}", userDetails.getUsername());
        User user = userService.findByEmail(userDetails.getUsername());

        AuthResponse.UserInfo userInfo = UserMapper.toUserInfo(user);

        return ResponseEntity.ok(ApiResponse.success("Profil récupéré avec succès", userInfo));
    }

    // ── CRUD Admin ──────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        log.debug("Récupération de la liste des utilisateurs");
        List<UserDto> users = userService.findAll().stream()
                .map(UserMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Utilisateurs récupérés", users));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllCustomers() {
        log.debug("Récupération de la liste des clients");
        List<UserDto> clients = userService.findAllCustomer().stream()
                .map(UserMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Clients récupérés", clients));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody UserFormRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Création d'un utilisateur admin: {}", request.getEmail());
        Long currentUserId = userService.findByEmail(userDetails.getUsername()).getId();
        User user = userService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Utilisateur créé avec succès", UserMapper.toDto(user)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        log.debug("Récupération de l'utilisateur ID: {}", id);
        User user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur récupéré", UserMapper.toDto(user)));
    }

    // PATCH (et non PUT) : le formulaire de modification n'envoie/n'exige pas
    // systématiquement l'intégralité de la ressource (ex: mot de passe laissé
    // vide = conservé), ce qui correspond sémantiquement à une mise à jour
    // partielle plutôt qu'un remplacement complet.
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserFormRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Mise à jour de l'utilisateur ID: {}", id);
        Long currentUserId = userService.findByEmail(userDetails.getUsername()).getId();
        User user = userService.update(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur mis à jour avec succès", UserMapper.toDto(user)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Suppression de l'utilisateur ID: {}", id);
        Long currentUserId = userService.findByEmail(userDetails.getUsername()).getId();
        userService.delete(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur supprimé avec succès", null));
    }

}
