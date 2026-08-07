package com.vivid.controller;

import com.vivid.model.payloads.responses.ApiResponse;
import com.vivid.model.payloads.requests.ServicePriceCreateRequest;
import com.vivid.model.dto.ServicePriceDto;
import com.vivid.model.payloads.requests.ServicePriceUpdateRequest;
import com.vivid.service.ServicePriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour les tarifs de service (prix des services proposés,
 * par type de vêtement + type de service).
 * <p>
 * URL de base : /service-prices
 * <p>
 * Règles de sécurité :
 * - Lecture   : ADMIN, MANAGER, EMPLOYEE
 * - Création / modification (ajout des prix) : ADMIN, MANAGER uniquement
 * - Suppression : ADMIN uniquement (comme toute suppression de l'application)
 */
@Slf4j
@RestController
@RequestMapping("/service-prices")
@RequiredArgsConstructor
public class ServicePriceController {

    private final ServicePriceService servicePriceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ServicePriceDto>> createServicePrice(
            @Valid @RequestBody ServicePriceCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("User {} creating service price: {} / {}", userDetails.getUsername(),
                request.getClothingType(), request.getService());
        ServicePriceDto created = servicePriceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tarif de service créé avec succès", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ServicePriceDto>>> getAllServicePrices() {
        return ResponseEntity.ok(ApiResponse.success("Tarifs récupérés avec succès",
                servicePriceService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ServicePriceDto>> getServicePriceById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tarif récupéré avec succès",
                servicePriceService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ServicePriceDto>> updateServicePrice(
            @PathVariable Long id,
            @Valid @RequestBody ServicePriceUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("User {} updating service price {}", userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Tarif mis à jour avec succès",
                servicePriceService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteServicePrice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Admin {} deleting service price {}", userDetails.getUsername(), id);
        servicePriceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Tarif supprimé avec succès", null));
    }
}
