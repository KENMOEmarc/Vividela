package com.template.vivid.model.entity;

import com.template.vivid.model.enums.OrderStatus;
import com.template.vivid.model.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
@Getter
@Setter
@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_user_id", nullable = false)
    private User clientUser;

    @NotNull
    @Column(name = "deposit_date", nullable = false)
    private LocalDate depositDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "delivered_at")
    private LocalDate deliveredAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "notes", length = 1000)
    private String notes;

    @NotNull
    @ColumnDefault("0.00")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @ColumnDefault("0.00")
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "loyalty_points_used", nullable = false)
    private Integer loyaltyPointsUsed;

    // AJOUT : le programme de fidélité était purement décoratif (voir revue
    // de code, règle manquante n°12) : loyaltyPointsUsed n'était jamais
    // rapproché du solde réel, jamais déduit, et aucun point n'était jamais
    // crédité. Ce flag évite de traiter deux fois (déduction des points
    // utilisés + crédit des points gagnés) la même commande si elle transite
    // plusieurs fois par DELIVERED, ou si recalculateStatus() est rappelée.
    @NotNull
    @ColumnDefault("false")
    @Column(name = "loyalty_processed", nullable = false)
    private Boolean loyaltyProcessed;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    /**
     * AJOUT : montant net réellement dû par le client, remise et points de
     * fidélité utilisés déduits (voir revue de code — règle manquante n°14 :
     * "La remise n'est jamais réellement appliquée au montant de la
     * commande"). Champ calculé (non persisté) exposé via OrderDto, afin que
     * l'API et le PDF du reçu (ReceiptPdfGenerator) affichent exactement la
     * même valeur — la conversion points→montant est appliquée séparément
     * dans OrderServiceImpl (dépend de LoyaltyProperties, non accessible
     * depuis l'entité).
     * <p>
     * Ne prend en compte ici que {@code discountAmount} ; la déduction des
     * points de fidélité est ajoutée par l'appelant (voir
     * OrderServiceImpl#computeNetAmountDue).
     */
    @Transient
    public java.math.BigDecimal getGrossAmountAfterDiscount() {
        java.math.BigDecimal total = totalAmount != null ? totalAmount : java.math.BigDecimal.ZERO;
        java.math.BigDecimal discount = discountAmount != null ? discountAmount : java.math.BigDecimal.ZERO;
        java.math.BigDecimal net = total.subtract(discount);
        return net.compareTo(java.math.BigDecimal.ZERO) < 0 ? java.math.BigDecimal.ZERO : net;
    }
}