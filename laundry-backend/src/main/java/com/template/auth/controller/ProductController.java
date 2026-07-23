package com.template.auth.controller;

import com.template.auth.model.dto.ApiResponse;
import com.template.auth.model.dto.ProductCreateRequest;
import com.template.auth.model.dto.ProductDto;
import com.template.auth.model.dto.ProductUpdateRequest;
import com.template.auth.model.entity.Product;
import com.template.auth.service.ProductService;
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
 * REST Controller for Product CRUD operations.
 *
 * Base URL: /api/products
 *
 * Security Rules:
 * - ADMIN:    Full CRUD access
 * - EMPLOYEE: Can create and update products
 * - CUSTOMER: No direct product management (catalog via orders only)
 *
 * BUGFIX : tous les try/catch locaux capturant IllegalArgumentException ont été
 * supprimés — GlobalExceptionHandler (handleIllegalArgument) centralise désormais
 * ce cas et retourne un 400 uniforme, évitant la duplication de code.
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("User {} creating new product: {}", userDetails.getUsername(), request.getName());
        Product product = productService.createProduct(request);
        ProductDto productDto = productService.getProductById(product.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Produit créé avec succès", productDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ProductDto>> getProductById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("User {} fetching product {}", userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Produit récupéré avec succès",
                productService.getProductById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllProducts(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("User {} fetching all products", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Produits récupérés avec succès",
                productService.getAllProducts()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("User {} updating product {}", userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Produit mis à jour avec succès",
                productService.updateProduct(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Admin {} deleting product {}", userDetails.getUsername(), id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Produit supprimé avec succès", null));
    }
}