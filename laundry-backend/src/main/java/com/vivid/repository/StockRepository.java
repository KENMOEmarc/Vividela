package com.vivid.repository;

import com.vivid.model.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * Tous les lots de stock d'un produit (ordre naturel).
     */
    List<Stock> findByProductId(Long productId);

    /**
     * Lots d'un produit triés selon la stratégie FEFO (First Expired, First Out) :
     * les lots dont la date d'expiration est la plus proche sortent en premier ;
     * les lots sans date d'expiration passent en dernier.
     */
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId " +
            "ORDER BY CASE WHEN s.expirationDate IS NULL THEN 1 ELSE 0 END, s.expirationDate ASC, s.entryDate ASC")
    List<Stock> findByProductIdOrderByExpirationFefo(@Param("productId") Long productId);

    /**
     * Somme des quantités de tous les lots d'un produit (0 si aucun lot).
     */
    @Query("SELECT COALESCE(SUM(s.currentQuantity), 0) FROM Stock s WHERE s.product.id = :productId")
    BigDecimal sumQuantityByProductId(@Param("productId") Long productId);


    /**
     * Lots encore en stock (quantité > 0) qui expirent avant la date donnée (toutes produits confondus).
     */
    @Query("SELECT s FROM Stock s WHERE s.expirationDate IS NOT NULL AND s.expirationDate <= :limitDate " +
            "AND s.currentQuantity > 0 ORDER BY s.expirationDate ASC")
    List<Stock> findExpiringBefore(@Param("limitDate") LocalDate limitDate);
}
