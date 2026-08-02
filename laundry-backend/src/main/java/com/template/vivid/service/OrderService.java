package com.template.vivid.service;

import com.template.vivid.model.payloads.requests.DepositRequest;
import com.template.vivid.model.payloads.requests.OrderCreateRequest;
import com.template.vivid.model.dto.OrderDto;
import com.template.vivid.model.payloads.requests.OrderUpdateRequest;
import com.template.vivid.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    OrderDto createOrder(OrderCreateRequest request, Long currentUserId);

    OrderDto getOrderById(Long id, boolean includeArticles);

    List<OrderDto> getAllOrders();

    /**
     * Récupère les commandes dont le statut figure dans la liste fournie.
     * Utilisé pour le filtrage par checkboxs de statut côté frontend.
     * Si la liste est vide ou nulle, retourne toutes les commandes.
     */
    List<OrderDto> getOrdersByStatuses(List<OrderStatus> statuses);

    List<OrderDto> getOrdersByClient(Long clientId);

    OrderDto updateOrder(Long id, OrderUpdateRequest request, Long currentUserId);

    void deleteOrder(Long id);

    /**
     * Recalcule le montant total d'une commande à partir des prix appliqués
     * de tous les services de tous ses articles, et persiste le résultat.
     */
    void recalculateTotal(Long orderId);

    /**
     * Recalcule automatiquement le statut d'une commande à partir du statut
     * de ses articles :
     * - tous les articles à PENDING     → commande PENDING
     * - tous les articles à COMPLETED   → commande READY
     * - sinon (traitement en cours)     → commande IN_PROGRESS
     * <p>
     * N'a aucun effet si la commande n'a pas encore d'article, ou si son
     * statut a déjà dépassé le stade READY (READY, DELIVERED, CANCELLED),
     * ces statuts étant alors gérés manuellement.
     */
    void recalculateStatus(Long orderId);

    /**
     * AJOUT : annule une commande, y compris AVANT qu'elle n'atteigne le
     * stade READY (dépôt annulé alors que les articles sont encore
     * RECEIVED/PENDING/IN_PROGRESS) — voir revue de code, règle manquante
     * n°10. Contrairement à updateOrder(), qui n'autorise un changement de
     * statut manuel qu'à partir de READY, cette méthode dédiée permet
     * explicitement cette transition ciblée, sans ouvrir la porte à
     * n'importe quel autre changement de statut manuel prématuré.
     * <p>
     * Refuse l'annulation si la commande est déjà DELIVERED ou déjà CANCELLED.
     */
    OrderDto cancelOrder(Long id, Long currentUserId);

    /**
     * AJOUT : montant net réellement dû pour une commande — total des
     * prestations, remise ({@code discountAmount}) ET points de fidélité
     * utilisés déduits (voir LoyaltyProperties). Utilisé par
     * PaymentServiceImpl pour le contrôle de surpaiement et la réconciliation
     * automatique de paymentStatus.
     */
    BigDecimal getNetAmountDue(Long orderId);

    /**
     * AJOUT : flux de dépôt atomique (voir revue de code, règle manquante
     * n°18) — crée la commande ET tous ses articles en une seule transaction,
     * au lieu d'exiger deux appels séparés (POST /orders puis
     * POST /orders/{id}/items par article) non transactionnels du point de
     * vue de l'appelant.
     */
    OrderDto createDeposit(DepositRequest request, Long currentUserId);

    /**
     * AJOUT : recherche d'une commande par le code-barres de son ticket
     * (voir revue de code, règle manquante n°15 — DTO BarcodeRequest orphelin).
     */
    OrderDto findOrderByBarcode(String barcode);
}
