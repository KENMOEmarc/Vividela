package com.template.vivid.repository;

import com.template.vivid.model.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByOrderId(Long orderId);

    /**
     * Charge en une seule requête les tickets de plusieurs commandes.
     * Utilisée par OrderServiceImpl pour éviter une requête findByOrderId
     * par commande lors du mappage d'une liste de commandes en DTO.
     */
    List<Ticket> findByOrderIdIn(List<Long> orderIds);

    /**
     * AJOUT : recherche d'un ticket par son code-barres, pour retrouver
     * directement une commande au guichet (voir revue de code, règle
     * manquante n°15 — DTO BarcodeRequest jusque-là orphelin).
     */
    Optional<Ticket> findByBarcode(String barcode);
}
