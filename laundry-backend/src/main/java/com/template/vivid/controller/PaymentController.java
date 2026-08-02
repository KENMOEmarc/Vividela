package com.template.vivid.controller;

import com.template.vivid.model.payloads.responses.ApiResponse;
import com.template.vivid.model.dto.PaymentDto;
import com.template.vivid.model.payloads.requests.PaymentRequest;
import com.template.vivid.service.PaymentService;
import com.template.vivid.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller — Paiements
 * <p>
 * Endpoints pour l'enregistrement et la gestion des paiements d'une commande.
 */
@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    /**
     * POST /payments — enregistre un paiement pour une commande
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PaymentDto>> recordPayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = resolveUserId(userDetails);
        PaymentDto created = paymentService.recordPayment(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Paiement enregistré avec succès", created));
    }

    /**
     * AJOUT : PATCH /payments/{id}/confirm — confirme l'encaissement effectif
     * d'un paiement PENDING. Voir revue de code, règles manquantes n°3 et n°4.
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PaymentDto>> confirmPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = resolveUserId(userDetails);
        return ResponseEntity.ok(
                ApiResponse.success("Paiement confirmé avec succès", paymentService.confirmPayment(id, currentUserId)));
    }

    /**
     * AJOUT : PATCH /payments/{id}/fail — marque un paiement PENDING comme échoué.
     * Voir revue de code, règle manquante n°4.
     */
    @PatchMapping("/{id}/fail")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PaymentDto>> failPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = resolveUserId(userDetails);
        return ResponseEntity.ok(
                ApiResponse.success("Paiement marqué comme échoué", paymentService.failPayment(id, currentUserId)));
    }

    /**
     * GET /payments/order/{orderId} — liste les paiements d'une commande.
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<java.util.List<PaymentDto>>> getPaymentsByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.success("Paiements de la commande récupérés", paymentService.getPaymentsByOrder(orderId)));
    }

    /**
     * Résout l'ID de l'utilisateur connecté depuis le contexte de sécurité.
     */
    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userService.findByEmail(userDetails.getUsername()).getId();
    }
}