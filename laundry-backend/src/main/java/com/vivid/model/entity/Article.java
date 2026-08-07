package com.vivid.model.entity;

import com.vivid.model.enums.ArticleStatus;
import com.vivid.model.enums.ClothingType;
import com.vivid.model.enums.FabricType;
import com.vivid.model.enums.SizeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "articles")
public class Article {
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
    @Column(name = "clothing_type", nullable = false)
    private ClothingType clothingType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false)
    private SizeType size;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "fabric", nullable = false)
    private FabricType fabric;

    /** Couleur stockée en VARCHAR(50) — ex: "#FF5733" ou "Rouge" */
    @Size(max = 50)
    @Column(name = "color", length = 50)
    private String color;

    @Size(max = 255)
    @Column(name = "distinction")
    private String distinction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Column(name = "status", nullable = false)
    private ArticleStatus status;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;
}