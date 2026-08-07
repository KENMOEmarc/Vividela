package com.vivid.service;

import com.vivid.model.dto.*;
import com.vivid.model.dto.StockBatchDto;
import com.vivid.model.dto.StockDto;
import com.vivid.model.dto.StockMovementDto;
import com.vivid.model.payloads.requests.StockBatchCreateRequest;
import com.vivid.model.payloads.requests.StockBatchUpdateRequest;
import com.vivid.model.payloads.requests.StockConsumptionRequest;

import java.util.List;

/**
 * Service de gestion du stock des produits.
 * <p>
 * Un produit peut posséder PLUSIEURS lots de stock (StockBatchDto), chacun
 * avec sa propre quantité, son prix d'achat, sa date d'entrée en stock et
 * sa date d'expiration. Ce service gère :
 * - la vue agrégée par produit (totaux, seuils, alertes)
 * - la gestion CRUD des lots individuels
 * - la consommation de stock (répartie automatiquement sur les lots
 * existants selon la stratégie FEFO — First Expired, First Out)
 * - l'historique des mouvements et des enregistrements
 */
public interface StockService {

    /**
     * Liste agrégée de tous les stocks (un total par produit), avec état (quantité, seuil, alerte).
     */
    List<StockDto> getAllStocks();

    /**
     * Stock agrégé d'un produit spécifique, avec le détail de ses lots.
     */
    StockDto getStockByProductId(Long productId);

    /**
     * Détail des lots de stock d'un produit (triés par date d'expiration croissante).
     */
    List<StockBatchDto> getBatchesByProduct(Long productId);

    /**
     * Crée un nouveau lot de stock pour un produit (réapprovisionnement).
     */
    StockBatchDto createBatch(Long currentUserId, StockBatchCreateRequest request);

    /**
     * Corrige manuellement un lot existant (quantité, prix, dates).
     */
    StockBatchDto updateBatch(Long currentUserId, Long batchId, StockBatchUpdateRequest request);

    /**
     * Supprime un lot de stock (uniquement si sa quantité restante est nulle).
     */
    void deleteBatch(Long currentUserId, Long batchId);

    /**
     * Consomme une quantité de stock pour un produit, répartie automatiquement
     * sur les lots existants (FEFO : le lot qui expire le plus tôt est
     * consommé en premier). Retourne l'état agrégé du produit après consommation.
     */
    StockDto consume(Long currentUserId, StockConsumptionRequest request);

    /**
     * Historique des mouvements pour un produit (tous lots confondus).
     */
    List<StockMovementDto> getMovementsByProduct(Long productId);

    /**
     * Tous les enregistrements de produits (entrées / ajustements)
     */
    List<ProductRegistrationDto> getAllRegistrations();

    /**
     * Enregistrements pour un produit spécifique
     */
    List<ProductRegistrationDto> getRegistrationsByProduct(Long productId);

    /**
     * Produits dont le stock total est en dessous du seuil d'alerte
     */
    List<StockDto> getLowStockProducts();

    /**
     * Lots dont la date d'expiration approche (dans les {@code days} prochains jours).
     */
    List<StockBatchDto> getExpiringBatches(int days);
}
