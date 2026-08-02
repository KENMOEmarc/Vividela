package com.template.vivid.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Représente un LOT (batch) de stock pour un produit.
 *
 * ÉVOLUTION : un produit peut désormais posséder PLUSIEURS lots de stock
 * (relation ManyToOne au lieu de OneToOne), chacun avec sa propre quantité,
 * son prix unitaire, sa date d'entrée en stock et sa date d'expiration.
 * Cela permet par exemple de suivre deux livraisons du même produit achetées
 * à des prix différents ou ayant des dates de péremption différentes.
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "stocks")
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Quantité restante dans CE lot précis. */
    @NotNull
    @ColumnDefault("0.00")
    @Column(name = "current_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentQuantity;

    /** Prix unitaire d'achat de ce lot (facultatif). */
    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Date d'entrée en stock (réception) de ce lot. */
    @NotNull
    @ColumnDefault("CURRENT_DATE")
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    /** Date d'expiration / péremption de ce lot (facultative : certains produits ne périment pas). */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @CreationTimestamp
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

}
