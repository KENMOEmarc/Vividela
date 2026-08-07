package com.vivid.service.impl;

import com.vivid.model.payloads.requests.ProductCreateRequest;
import com.vivid.model.dto.ProductDto;
import com.vivid.model.payloads.requests.ProductUpdateRequest;
import com.vivid.model.entity.Product;
import com.vivid.model.enums.MeasurementUnit;
import com.vivid.model.mapper.ProductMapper;
import com.vivid.repository.ProductRegistrationRepository;
import com.vivid.repository.ProductRepository;
import com.vivid.repository.StockRepository;
import com.vivid.service.ProductService;
import com.vivid.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import com.vivid.exception.ResourceNotFoundException;
import com.vivid.exception.DuplicateResourceException;
import com.vivid.exception.InvalidStateTransitionException;

/**
 * Service implementation for Product CRUD operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRegistrationRepository productRegistrationRepository;

    @Override
    public Product createProduct(ProductCreateRequest request) {
        log.debug("Creating new product: {}", request.getName());

        if (productRepository.findByNameIgnoreCase(request.getName()).isPresent()) {
            throw new DuplicateResourceException("Un produit avec ce nom existe déjà: " + request.getName());
        }

        Product product = Product.builder()
                .name(request.getName())
                .thresholdValue(request.getThresholdValue())
                .measurementUnit(MeasurementUnit.valueOf(request.getMeasurementUnit()))
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        // NOTE : le stock agrégé est désormais calculé à la volée à partir des
        // lots (Stock) du produit, il n'est donc plus nécessaire de créer une
        // ligne de stock "placeholder" à quantité 0 : un produit sans lot
        // apparaît naturellement avec une quantité totale de 0 dans la page Stock.

        return savedProduct;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        log.debug("Fetching product by ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'ID: " + id));

        return ProductMapper.toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        log.debug("Fetching all products");

        List<Product> products = productRepository.findAll();
        log.debug("Found {} products", products.size());

        return products.stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDto updateProduct(Long id, ProductUpdateRequest request) {
        log.debug("Updating product ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'ID: " + id));

        if (!product.getName().equalsIgnoreCase(request.getName())) {
            if (productRepository.findByNameIgnoreCase(request.getName()).isPresent()) {
                throw new DuplicateResourceException("Un produit avec ce nom existe déjà: " + request.getName());
            }
        }

        product.setName(request.getName());
        product.setThresholdValue(request.getThresholdValue());
        product.setMeasurementUnit(MeasurementUnit.valueOf(request.getMeasurementUnit()));

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with ID: {}", updatedProduct.getId());

        return ProductMapper.toDto(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        log.debug("Deleting product ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'ID: " + id));

        // AJOUT : empêche la suppression physique d'un produit dont le stock
        // n'est pas à zéro, ou qui possède un historique de mouvements/
        // approvisionnements — même garde-fou que deleteOrder() pour
        // préserver la traçabilité. Voir revue de code, règle manquante n°1
        // (section Stock & Produits).
        BigDecimal totalQuantity = stockRepository.sumQuantityByProductId(id);
        if (totalQuantity != null && totalQuantity.compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidStateTransitionException(
                    "Impossible de supprimer le produit '" + product.getName() + "' : son stock n'est pas à "
                            + "zéro (quantité actuelle : " + totalQuantity + " sur l'ensemble des lots). Videz le "
                            + "stock (consommez tous les lots) avant de supprimer le produit.");
        }
        if (stockMovementRepository.existsByStock_Product_Id(id) || productRegistrationRepository.existsByProductId(id)) {
            throw new InvalidStateTransitionException(
                    "Impossible de supprimer le produit '" + product.getName() + "' : il possède un historique de "
                            + "mouvements de stock ou d'approvisionnements. Supprimer ce produit ferait perdre "
                            + "cette traçabilité.");
        }

        productRepository.delete(product);
        log.info("Product deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Product findByName(String name) {
        return productRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec le nom: " + name));
    }

}
