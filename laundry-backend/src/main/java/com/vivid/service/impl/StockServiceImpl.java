package com.vivid.service.impl;

import com.vivid.model.dto.*;
import com.vivid.model.entity.*;
import com.vivid.model.dto.ProductRegistrationDto;
import com.vivid.model.dto.StockBatchDto;
import com.vivid.model.dto.StockDto;
import com.vivid.model.dto.StockMovementDto;
import com.vivid.model.entity.*;
import com.vivid.model.enums.MovementType;
import com.vivid.model.enums.RegistrationType;
import com.vivid.model.mapper.StockMapper;
import com.vivid.model.payloads.requests.StockBatchCreateRequest;
import com.vivid.model.payloads.requests.StockBatchUpdateRequest;
import com.vivid.model.payloads.requests.StockConsumptionRequest;
import com.vivid.repository.*;
import com.vivid.service.NotificationService;
import com.vivid.service.StockService;
import com.vivid.common.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.vivid.exception.ResourceNotFoundException;
import com.vivid.exception.InvalidRequestException;
import com.vivid.exception.InvalidStateTransitionException;
import com.vivid.exception.InsufficientStockException;

/**
 * Gère le stock d'un produit, désormais réparti sur PLUSIEURS lots (Stock)
 * ayant chacun leur quantité, prix d'achat, date d'entrée et date
 * d'expiration. Les entrées créent un nouveau lot ; les consommations sont
 * réparties automatiquement sur les lots existants selon la stratégie FEFO
 * (First Expired, First Out).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRegistrationRepository productRegistrationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ─────────────────────────────────── READ ────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<StockDto> getAllStocks() {
        return productRepository.findAll().stream()
                .map(product -> StockMapper.toAggregatedDto(
                        product, stockRepository.findByProductId(product.getId()), false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StockDto getStockByProductId(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'ID: " + productId));
        List<Stock> batches = stockRepository.findByProductIdOrderByExpirationFefo(productId);
        return StockMapper.toAggregatedDto(product, batches, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchDto> getBatchesByProduct(Long productId) {
        return StockMapper.toBatchDtoList(stockRepository.findByProductIdOrderByExpirationFefo(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockDto> getLowStockProducts() {
        return productRepository.findAll().stream()
                .map(product -> StockMapper.toAggregatedDto(
                        product, stockRepository.findByProductId(product.getId()), false))
                .filter(StockDto::isBelowThreshold)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByProduct(Long productId) {
        return stockMovementRepository.findByProductIdOrderByDateDesc(productId).stream()
                .map(StockMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductRegistrationDto> getAllRegistrations() {
        return productRegistrationRepository.findAllByOrderByRegisteredAtDesc().stream()
                .map(StockMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductRegistrationDto> getRegistrationsByProduct(Long productId) {
        return productRegistrationRepository.findByProductIdOrderByRegisteredAtDesc(productId).stream()
                .map(StockMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchDto> getExpiringBatches(int days) {
        LocalDate limit = LocalDate.now().plusDays(Math.max(days, 0));
        return StockMapper.toBatchDtoList(stockRepository.findExpiringBefore(limit));
    }

    // ──────────────────────────────── WRITE ──────────────────────────────────

    @Override
    public StockBatchDto createBatch(Long currentUserId, StockBatchCreateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable: " + request.getProductId()));
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("La quantité du lot doit être strictement positive.");
        }
        if (request.getExpirationDate() != null && request.getEntryDate() != null
                && request.getExpirationDate().isBefore(request.getEntryDate())) {
            throw new InvalidRequestException("La date d'expiration ne peut pas être antérieure à la date d'entrée en stock.");
        }

        BigDecimal previousTotal = stockRepository.sumQuantityByProductId(product.getId());

        Stock batch = new Stock();
        batch.setProduct(product);
        batch.setCurrentQuantity(request.getQuantity());
        batch.setUnitPrice(request.getUnitPrice());
        batch.setEntryDate(request.getEntryDate() != null ? request.getEntryDate() : LocalDate.now());
        batch.setExpirationDate(request.getExpirationDate());
        Stock savedBatch = stockRepository.save(batch);

        recordMovement(savedBatch, user, request.getQuantity(), MovementType.RESTOCK, request.getNotes());
        recordRegistration(product, user, request.getQuantity(), RegistrationType.IN, request.getNotes());

        log.info("Nouveau lot de stock créé pour le produit {} (qté={}, expiration={})",
                product.getName(), request.getQuantity(), request.getExpirationDate());

        checkAndNotifyThresholdCrossing(product, previousTotal);

        return StockMapper.toBatchDto(savedBatch);
    }

    @Override
    public StockBatchDto updateBatch(Long currentUserId, Long batchId, StockBatchUpdateRequest request) {
        Stock batch = stockRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot de stock introuvable: " + batchId));
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Product product = batch.getProduct();

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("La quantité doit être positive ou nulle.");
        }
        if (request.getExpirationDate() != null && request.getEntryDate() != null
                && request.getExpirationDate().isBefore(request.getEntryDate())) {
            throw new InvalidRequestException("La date d'expiration ne peut pas être antérieure à la date d'entrée en stock.");
        }

        BigDecimal previousTotal = stockRepository.sumQuantityByProductId(product.getId());
        BigDecimal delta = request.getQuantity().subtract(batch.getCurrentQuantity());

        batch.setCurrentQuantity(request.getQuantity());
        if (request.getUnitPrice() != null) {
            batch.setUnitPrice(request.getUnitPrice());
        }
        if (request.getEntryDate() != null) {
            batch.setEntryDate(request.getEntryDate());
        }
        batch.setExpirationDate(request.getExpirationDate());
        Stock saved = stockRepository.save(batch);

        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            recordMovement(saved, user, delta.abs(), MovementType.ADJUSTMENT, request.getNotes());
            recordRegistration(product, user, delta.abs(), RegistrationType.ADJUSTMENT, request.getNotes());
        }

        log.info("Lot de stock {} corrigé pour le produit {} (nouvelle quantité={})",
                batchId, product.getName(), request.getQuantity());

        checkAndNotifyThresholdCrossing(product, previousTotal);

        return StockMapper.toBatchDto(saved);
    }

    @Override
    public void deleteBatch(Long currentUserId, Long batchId) {
        Stock batch = stockRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot de stock introuvable: " + batchId));

        if (batch.getCurrentQuantity() != null && batch.getCurrentQuantity().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidStateTransitionException(
                    "Impossible de supprimer ce lot : sa quantité restante n'est pas à zéro ("
                            + batch.getCurrentQuantity() + "). Consommez ou ajustez le lot à zéro avant de le supprimer.");
        }
        if (stockMovementRepository.existsByStockId(batchId)) {
            throw new InvalidStateTransitionException(
                    "Impossible de supprimer ce lot : il possède un historique de mouvements. "
                            + "Sa traçabilité doit être conservée.");
        }

        stockRepository.delete(batch);
        log.info("Lot de stock {} supprimé (utilisateur {})", batchId, currentUserId);
    }

    @Override
    public StockDto consume(Long currentUserId, StockConsumptionRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable: " + request.getProductId()));
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        BigDecimal quantityToConsume = request.getQuantity();
        if (quantityToConsume == null || quantityToConsume.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("La quantité consommée doit être strictement positive.");
        }

        List<Stock> batches = stockRepository.findByProductIdOrderByExpirationFefo(product.getId());
        BigDecimal previousTotal = batches.stream()
                .map(Stock::getCurrentQuantity)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (previousTotal.compareTo(quantityToConsume) < 0) {
            throw new InsufficientStockException(
                    "Stock insuffisant. Disponible: " + previousTotal + ", demandé: " + quantityToConsume);
        }

        BigDecimal remaining = quantityToConsume;
        List<Stock> touched = new ArrayList<>();
        for (Stock batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal available = batch.getCurrentQuantity() != null ? batch.getCurrentQuantity() : BigDecimal.ZERO;
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal taken = available.min(remaining);
            batch.setCurrentQuantity(available.subtract(taken));
            stockRepository.save(batch);
            recordMovement(batch, user, taken, MovementType.CONSUMPTION, request.getNotes());
            touched.add(batch);
            remaining = remaining.subtract(taken);
        }

        recordRegistration(product, user, quantityToConsume, RegistrationType.SACHET, request.getNotes());

        log.info("Consommation de {} {} pour le produit {} répartie sur {} lot(s)",
                quantityToConsume, product.getMeasurementUnit(), product.getName(), touched.size());

        checkAndNotifyThresholdCrossing(product, previousTotal);

        List<Stock> refreshed = stockRepository.findByProductIdOrderByExpirationFefo(product.getId());
        return StockMapper.toAggregatedDto(product, refreshed, true);
    }

    // ──────────────────────────────── HELPERS ─────────────────────────────────

    private void recordMovement(Stock stock, User user, BigDecimal quantity, MovementType type, String notes) {
        StockMovement movement = new StockMovement();
        movement.setStock(stock);
        movement.setUser(user);
        movement.setQuantity(quantity);
        movement.setMovementType(type);
        movement.setNotes(notes);
        movement.setMovementDate(Instant.now());
        stockMovementRepository.save(movement);
    }

    private void recordRegistration(Product product, User user, BigDecimal quantity, RegistrationType type, String notes) {
        ProductRegistration reg = new ProductRegistration();
        reg.setProduct(product);
        reg.setEmployeeUser(user);
        reg.setQuantity(quantity);
        reg.setRegistrationType(type);
        reg.setNotes(notes);
        reg.setRegisteredAt(Instant.now());
        productRegistrationRepository.save(reg);
    }

    /**
     * Envoie une alerte de stock bas uniquement lors du PASSAGE sous le seuil
     * (évite de notifier à chaque mouvement tant que le stock reste bas).
     */
    private void checkAndNotifyThresholdCrossing(Product product, BigDecimal previousTotal) {
        BigDecimal newTotal = stockRepository.sumQuantityByProductId(product.getId());
        boolean wasAboveThreshold = previousTotal.compareTo(product.getThresholdValue()) > 0;
        boolean isNowAtOrBelowThreshold = newTotal.compareTo(product.getThresholdValue()) <= 0;
        if (wasAboveThreshold && isNowAtOrBelowThreshold) {
            TransactionUtils.runAfterCommit(() -> notificationService.notifyLowStock(product, newTotal));
        }
    }
}
