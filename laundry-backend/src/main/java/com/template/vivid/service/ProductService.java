package com.template.vivid.service;

import com.template.vivid.model.dto.ProductCreateRequest;
import com.template.vivid.model.dto.ProductDto;
import com.template.vivid.model.dto.ProductUpdateRequest;
import com.template.vivid.model.entity.Product;

import java.util.List;

/**
 * Service interface for Product CRUD operations
 *
 * Business Rules:
 * - ADMIN: can perform all operations
 * - EMPLOYEE: can create and update products
 * - CUSTOMER: can only read products (catalog view)
 */
public interface ProductService {

    /**
     * Create a new product
     */
    Product createProduct(ProductCreateRequest request);

    /**
     * Get product by ID
     */
    ProductDto getProductById(Long id);

    /**
     * Get all products
     */
    List<ProductDto> getAllProducts();

    /**
     * Update an existing product
     */
    ProductDto updateProduct(Long id, ProductUpdateRequest request);

    /**
     * Delete a product (admin only)
     */
    void deleteProduct(Long id);

    /**
     * Get product entity by ID (internal use)
     */
    Product findById(Long id);

    /**
     * Get product by name
     */
    Product findByName(String name);
}

