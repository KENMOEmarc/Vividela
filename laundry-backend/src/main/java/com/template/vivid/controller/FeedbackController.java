package com.template.vivid.controller;

import com.template.vivid.model.dto.ApiResponse;
import com.template.vivid.model.dto.FeedbackDto;
import com.template.vivid.model.dto.FeedbackSubmitRequest;
import com.template.vivid.service.FeedbackService;
import com.template.vivid.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST du formulaire d'avis client rattaché à une commande.
 * <p>
 * URL de base : /orders/{orderId}/feedback  (le préfixe /api est ajouté par
 * context-path, voir application.yml).
 * <p>
 * Flux : une fois une commande livrée, un formulaire "vide" est généré côté
 * serveur (voir FeedbackServiceImpl#requestFeedback, déclenché depuis
 * OrderServiceImpl#updateOrder) et le client en est notifié. Ce contrôleur
 * permet au client de consulter ce formulaire (GET) puis de le soumettre
 * (POST) ; l'avis soumis est alors analysé par IA (Gemini, via Spring AI)
 * et transmis au manager (et à l'admin si négatif) — voir NotificationService.
 */
@Slf4j
@RestController
@RequestMapping("/orders/{orderId}/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserService userService;

    /**
     * GET /orders/{orderId}/feedback
     * Retourne le formulaire d'avis de la commande (état "demandé" ou déjà
     * "soumis"), ou {@code data: null} si la commande n'a pas encore été
     * livrée (aucun formulaire généré). Accessible au client propriétaire de
     * la commande, ou au personnel.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<FeedbackDto>> getFeedback(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        FeedbackDto dto = feedbackService.getFeedbackForOrder(orderId, resolveUserId(userDetails), isStaff(userDetails));
        return ResponseEntity.ok(ApiResponse.success("Formulaire d'avis récupéré", dto));
    }

    /**
     * POST /orders/{orderId}/feedback
     * Soumission de l'avis (note 1-5 + commentaire optionnel) par le client
     * propriétaire de la commande, une seule fois par commande livrée.
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<FeedbackDto>> submitFeedback(
            @PathVariable Long orderId,
            @Valid @RequestBody FeedbackSubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        FeedbackDto dto = feedbackService.submitFeedback(orderId, resolveUserId(userDetails), request);
        log.info("Avis soumis par le client pour la commande {}", orderId);
        return ResponseEntity.ok(ApiResponse.success("Merci pour votre avis !", dto));
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername()).getId();
    }

    private boolean isStaff(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_EMPLOYEE"));
    }
}
