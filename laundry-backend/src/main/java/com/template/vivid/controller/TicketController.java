package com.template.vivid.controller;

import com.template.vivid.model.payloads.responses.ApiResponse;
import com.template.vivid.model.dto.GeneratedPdfDto;
import com.template.vivid.model.dto.OrderDto;
import com.template.vivid.model.dto.TicketDto;
import com.template.vivid.service.OrderService;
import com.template.vivid.service.TicketService;
import com.template.vivid.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour les tickets de dépôt et les reçus (PDF) d'une commande.
 * <p>
 * URL de base : /orders/{orderId}/ticket  (context-path=/api dans application.yml)
 * <p>
 * Un ticket ou un reçu ne peut être généré qu'une fois la commande créée ET au
 * moins un vêtement (article) enregistré dessus.
 */
@RestController
@RequestMapping("/orders/{orderId}/ticket")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final OrderService orderService;
    private final UserService userService;

    /**
     * Vérifie que l'appelant est membre du personnel, ou le client propriétaire
     * de la commande consultée. Lève AccessDeniedException (→ 403) sinon.
     */
    private void ensureStaffOrOwner(Long orderId, UserDetails userDetails) {
        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_EMPLOYEE"));
        if (isStaff) {
            return;
        }
        OrderDto order = orderService.getOrderById(orderId, false);
        Long currentUserId = userService.findByEmail(userDetails.getUsername()).getId();
        if (order.getClientUserId() == null || !order.getClientUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Accès refusé : vous ne pouvez consulter que vos propres tickets");
        }
    }

    /**
     * POST /orders/{orderId}/ticket — crée (ou renvoie s'il existe déjà) le ticket de la commande.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<TicketDto>> generateTicket(@PathVariable Long orderId) {
        TicketDto ticket = ticketService.generateTicket(orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ticket généré avec succès", ticket));
    }

    /**
     * GET /orders/{orderId}/ticket — informations du ticket existant.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<TicketDto>> getTicket(
            @PathVariable Long orderId, @AuthenticationPrincipal UserDetails userDetails) {
        ensureStaffOrOwner(orderId, userDetails);
        return ResponseEntity.ok(
                ApiResponse.success("Ticket récupéré", ticketService.getTicketByOrderId(orderId)));
    }

    /**
     * GET /orders/{orderId}/ticket/pdf — génère et télécharge le ticket au format PDF.
     */
    // No 'produces' declared here to avoid 406 Not Acceptable when the client
    // sends an Accept header that doesn't include 'application/pdf'.
    // The method sets the Content-Type header explicitly on the response.
    @GetMapping(value = "/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<byte[]> downloadTicketPdf(
            @PathVariable Long orderId, @AuthenticationPrincipal UserDetails userDetails) {
        ensureStaffOrOwner(orderId, userDetails);
        GeneratedPdfDto doc = ticketService.generateTicketPdf(orderId);

        // Le nom de fichier correspond exactement au numéro imprimé sur le document,
        // et la disposition "inline" permet au navigateur de l'afficher (nouvel onglet)
        // plutôt que de forcer un téléchargement.
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(doc.fileName())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        headers.setContentType(MediaType.APPLICATION_PDF);

        return ResponseEntity.ok()
                .headers(headers)
                .body(doc.getContent());
    }

    /**
     * GET /orders/{orderId}/ticket/receipt — génère et télécharge le reçu de la commande au format PDF.
     * <p>
     * Comme pour le ticket, le reçu ne peut être généré qu'une fois la commande créée
     * ET au moins un vêtement (article) enregistré dessus.
     */
    @GetMapping(value = "/receipt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<byte[]> downloadReceiptPdf(
            @PathVariable Long orderId, @AuthenticationPrincipal UserDetails userDetails) {
        ensureStaffOrOwner(orderId, userDetails);
        GeneratedPdfDto doc = ticketService.generateReceiptPdf(orderId, userDetails.getUsername());

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(doc.fileName())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        headers.setContentType(MediaType.APPLICATION_PDF);

        return ResponseEntity.ok()
                .headers(headers)
                .body(doc.getContent());
    }
}
