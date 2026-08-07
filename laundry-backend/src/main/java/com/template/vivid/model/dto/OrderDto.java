package com.vivid.model.dto;

import com.vivid.model.enums.OrderStatus;
import com.vivid.model.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Builder
public record OrderDto(
        Long id,
        Long clientUserId,
        String userName,
        String ticketNumber,
        String customerName,
        String customerLastName,
        String customerEmail,
        String customerPhone,
        LocalDate depositDate,
        LocalDate expectedDeliveryDate,
        LocalDate deliveredAt,
        OrderStatus status,
        PaymentStatus paymentStatus,
        String shippingAddress,
        String notes,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        /**
         * AJOUT : montant net réellement dû (totalAmount - discountAmount -
         * valeur des points de fidélité utilisés), calculé par OrderServiceImpl
         * pour que l'API expose exactement la même valeur que le PDF du reçu.
         * Voir revue de code, règle manquante n°14.
         */
        BigDecimal netAmountDue,
        Integer loyaltyPointsUsed,
        /**
         * AJOUT : état du formulaire d'avis client pour cette commande —
         * {@code null} tant qu'elle n'est pas livrée (aucun formulaire généré),
         * {@code "REQUESTED"} une fois livrée et le formulaire envoyé au client
         * mais pas encore rempli, {@code "SUBMITTED"} une fois l'avis soumis.
         * Permet au frontend d'afficher (ou non) le bouton "Donner mon avis"
         * sans appel API supplémentaire par commande. Voir FeedbackService.
         */
        String feedbackStatus,
        Integer itemCount,
        Instant orderDate,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy,
        Long updatedBy,
        List<ArticleDto> articles // optionnel, pour les détails
) {
}