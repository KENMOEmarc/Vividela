package com.template.vivid.controller;

import com.template.vivid.model.dto.ApiResponse;
import com.template.vivid.model.dto.AuthResponse;
import com.template.vivid.model.dto.UserDto;
import com.template.vivid.model.dto.UserFormRequest;
import com.template.vivid.model.entity.User;
import com.template.vivid.model.mapper.UserMapper;
import com.template.vivid.repository.UserRepository;
import com.template.vivid.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour les opérations sur les utilisateurs.
 * <p>
 * URL DE BASE : /api/users
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
    private final UserRepository userRepository;

    // ── Profil personnel ────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Récupération du profil pour: {}", userDetails.getUsername());
        // Utilisation de findByEmail du repository pour obtenir l'entité User brute,
        // nécessaire pour la conversion en UserInfo (pas via le service qui retourne UserDto)
        User user = userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        AuthResponse.UserInfo userInfo = UserMapper.toUserInfo(user);

        return ResponseEntity.ok(ApiResponse.success("Profil récupéré avec succès", userInfo));
    }

    // ── CRUD Admin ──────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        log.debug("Récupération de la liste des utilisateurs");
        List<UserDto> users = userService.findAll();
        return ResponseEntity.ok(ApiResponse.success("Utilisateurs récupérés", users));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllCustomers() {
        log.debug("Récupération de la liste des clients");
        List<UserDto> clients = userService.findAllCustomer();
        return ResponseEntity.ok(ApiResponse.success("Clients récupérés", clients));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody UserFormRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Création d'un utilisateur admin: {}", request.getEmail());
        User currentUser = userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        UserDto user = userService.create(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Utilisateur créé avec succès", user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        log.debug("Récupération de l'utilisateur ID: {}", id);
        UserDto user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur récupéré", user));
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
        User currentUser = userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        UserDto user = userService.update(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Utilisateur mis à jour avec succès", user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Suppression de l'utilisateur ID: {}", id);
        User currentUser = userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        userService.delete(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Utilisateur supprimé avec succès", null));
    }

}
