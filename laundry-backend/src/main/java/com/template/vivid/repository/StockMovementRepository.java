package com.template.vivid.repository;

import com.template.vivid.model.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("SELECT sm FROM StockMovement sm WHERE sm.stock.product.id = :productId ORDER BY sm.movementDate DESC")
    List<StockMovement> findByProductIdOrderByDateDesc(@Param("productId") Long productId);


    /**
     * AJOUT : vérifie l'existence de mouvements de stock historiques pour un
     * produit, utilisée pour empêcher la suppression d'un produit dont la
     * traçabilité serait perdue. Voir revue de code, règle manquante n°1
     * (section Stock & Produits).
     */
    boolean existsByStock_Product_Id(Long productId);

    /** Vérifie si un lot de stock précis possède déjà des mouvements historiques. */
    boolean existsByStockId(Long stockId);
}
