package com.template.auth.model.entity;

import com.template.auth.model.enums.PaymentMethodType;
import com.template.auth.model.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethodType paymentMethod;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Size(max = 20)
    @Column(name = "payer_phone", length = 20)
    private String payerPhone;

    @Size(max = 255)
    @Column(name = "transaction_reference")
    private String transactionReference;

    // BUGFIX : ce champ était typé RequestStatus (PENDING/FAILED/SUCCESS), qui
    // ne correspond ni sémantiquement ni en valeurs à la colonne "status" de la
    // table "payments" (ENUM('PENDING','COMPLETED','FAILED','REFUNDED') dans
    // laundry.sql) — ce qui provoquait un IllegalArgumentException ("No enum
    // constant ... RequestStatus.COMPLETED") dès qu'une ligne existante était
    // relue depuis la base. PaymentStatus est l'enum dédiée qui correspond
    // exactement au schéma SQL.
    @NotNull
    @ColumnDefault("'PENDING'")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "paid_at")
    private Instant paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "created_by")
    private User createdBy;


}