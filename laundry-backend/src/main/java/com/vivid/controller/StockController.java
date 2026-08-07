package com.vivid.controller;

import com.vivid.model.dto.*;
import com.vivid.model.dto.StockBatchDto;
import com.vivid.model.dto.StockDto;
import com.vivid.model.dto.StockMovementDto;
import com.vivid.model.payloads.responses.ApiResponse;
import com.vivid.model.payloads.requests.StockBatchCreateRequest;
import com.vivid.model.payloads.requests.StockBatchUpdateRequest;
import com.vivid.model.payloads.requests.StockConsumptionRequest;
import com.vivid.security.UserDetailsServiceImpl;
import com.vivid.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — Gestion du Stock
 * <p>
 * Base URL: /api/stock
 * <p>
 * Un produit peut posséder PLUSIEURS lots de stock (batches), chacun avec sa
 * propre quantité, son prix d'achat, sa date d'entrée en stock et sa date
 * d'expiration.
 * <p>
 * Endpoints:
 * GET    /api/stock                          → vue agrégée (totaux) de tous les produits
 * GET    /api/stock/low                      → produits en alerte (total sous le seuil)
 * GET    /api/stock/product/{productId}      → vue agrégée d'un produit + détail des lots
 * GET    /api/stock/product/{productId}/batches → liste des lots d'un produit
 * POST   /api/stock/batches                  → créer un nouveau lot (réapprovisionnement)
 * PUT    /api/stock/batches/{batchId}        → corriger un lot existant
 * DELETE /api/stock/batches/{batchId}        → supprimer un lot (quantité nulle uniquement)
 * POST   /api/stock/consume                  → consommer du stock (réparti en FEFO)
 * GET    /api/stock/expiring?days=7          → lots expirant bientôt
 * GET    /api/stock/movements/{productId}    → historique mouvements
 * GET    /api/stock/registrations            → tous les enregistrements
 * GET    /api/stock/registrations/{productId} → enregistrements par produit
 */
@Slf4j
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final UserDetailsServiceImpl userDetailsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<StockDto>>> getAllStocks(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} fetching all stocks", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Stocks récupérés avec succès",
                stockService.getAllStocks()));
    }

    @GetMapping("/low")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<StockDto>>> getLowStock(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} fetching low stock products", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Produits en alerte récupérés",
                stockService.getLowStockProducts()));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<StockDto>> getStockByProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} fetching stock for product {}", userDetails.getUsername(), productId);
        return ResponseEntity.ok(ApiResponse.success("Stock récupéré avec succès",
                stockService.getStockByProductId(productId)));
    }

    @GetMapping("/product/{productId}/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<StockBatchDto>>> getBatches(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} fetching batches for product {}", userDetails.getUsername(), productId);
        return ResponseEntity.ok(ApiResponse.success("Lots de stock récupérés",
                stockService.getBatchesByProduct(productId)));
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<StockBatchDto>> createBatch(
            @Valid @RequestBody StockBatchCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} creating stock batch for product {}", userDetails.getUsername(), request.getProductId());
        Long userId = userDetailsService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Lot de stock créé avec succès",
                stockService.createBatch(userId, request)));
    }

    @PutMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<StockBatchDto>> updateBatch(
            @PathVariable Long batchId,
            @Valid @RequestBody StockBatchUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} updating stock batch {}", userDetails.getUsername(), batchId);
        Long userId = userDetailsService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Lot de stock mis à jour avec succès",
                stockService.updateBatch(userId, batchId, request)));
    }

    @DeleteMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} deleting stock batch {}", userDetails.getUsername(), batchId);
        Long userId = userDetailsService.getUserIdByUsername(userDetails.getUsername());
        stockService.deleteBatch(userId, batchId);
        return ResponseEntity.ok(ApiResponse.success("Lot de stock supprimé avec succès"));
    }

    @PostMapping("/consume")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<StockDto>> consume(
            @Valid @RequestBody StockConsumptionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} consuming stock for product {}", userDetails.getUsername(), request.getProductId());
        Long userId = userDetailsService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Stock consommé avec succès",
                stockService.consume(userId, request)));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<StockBatchDto>>> getExpiringBatches(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} fetching batches expiring within {} days", userDetails.getUsername(), days);
        return ResponseEntity.ok(ApiResponse.success("Lots bientôt expirés récupérés",
                stockService.getExpiringBatches(days)));
    }

    @GetMapping("/movements/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getMovements(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} fetching movements for product {}", userDetails.getUsername(), productId);
        return ResponseEntity.ok(ApiResponse.success("Mouvements récupérés",
                stockService.getMovementsByProduct(productId)));
    }

    @GetMapping("/registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ProductRegistrationDto>>> getAllRegistrations(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} fetching all registrations", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Enregistrements récupérés",
                stockService.getAllRegistrations()));
    }

    @GetMapping("/registrations/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ProductRegistrationDto>>> getRegistrationsByProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("User {} fetching registrations for product {}", userDetails.getUsername(), productId);
        return ResponseEntity.ok(ApiResponse.success("Enregistrements récupérés",
                stockService.getRegistrationsByProduct(productId)));
    }
}
