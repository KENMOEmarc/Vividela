package com.template.auth.model.dto;

import com.template.auth.model.enums.OrderStatus;
import com.template.auth.model.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private Long clientUserId;
    private String userName;
    private String ticketNumber;
    private String customerName;
    private String customerLastName;
    private String customerEmail;
    private String customerPhone;
    private LocalDate depositDate;
    private LocalDate expectedDeliveryDate;
    private LocalDate deliveredAt;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private String notes;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    /**
     * AJOUT : montant net réellement dû (totalAmount - discountAmount -
     * valeur des points de fidélité utilisés), calculé par OrderServiceImpl
     * pour que l'API expose exactement la même valeur que le PDF du reçu.
     * Voir revue de code, règle manquante n°14.
     */
    private BigDecimal netAmountDue;
    private Integer loyaltyPointsUsed;
    /**
     * AJOUT : état du formulaire d'avis client pour cette commande —
     * {@code null} tant qu'elle n'est pas livrée (aucun formulaire généré),
     * {@code "REQUESTED"} une fois livrée et le formulaire envoyé au client
     * mais pas encore rempli, {@code "SUBMITTED"} une fois l'avis soumis.
     * Permet au frontend d'afficher (ou non) le bouton "Donner mon avis"
     * sans appel API supplémentaire par commande. Voir FeedbackService.
     */
    private String feedbackStatus;
    private Integer itemCount;
    private Instant orderDate;
    private Instant createdAt;
    private Instant updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private List<ArticleDto> articles; // optionnel, pour les détails
}