package com.vivid.repository;

import com.vivid.model.entity.ProductRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ProductRegistrationRepository extends JpaRepository<ProductRegistration, Long> {
    List<ProductRegistration> findByProductIdOrderByRegisteredAtDesc(Long productId);

    List<ProductRegistration> findAllByOrderByRegisteredAtDesc();

    /**
     * AJOUT : vérifie l'existence d'enregistrements d'approvisionnement
     * historiques pour un produit. Voir revue de code, règle manquante n°1
     * (section Stock & Produits).
     */
    boolean existsByProductId(Long productId);
}
