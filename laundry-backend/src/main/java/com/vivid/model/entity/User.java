package com.vivid.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vivid.model.enums.RoleType;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private RoleType role;

    @Size(max = 100)
    @NotNull
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Size(max = 100)
    @NotNull
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Size(max = 50)
    @NotNull
    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Size(max = 255)
    @NotNull
    @Column(name = "email", nullable = false)
    private String email;

    @Size(max = 20)
    @NotNull
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 255)
    @NotNull
    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    @ColumnDefault("0")
    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints;

    @ColumnDefault("1")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * BUGFIX : l'ancienne version mappait ce champ en @OneToOne sur la colonne
     * "id" elle-même (entrant en conflit avec la clé primaire de l'entité),
     * en NOT NULL (optional = false) — ce qui faisait planter toute création
     * d'utilisateur sans "créateur" (ex: inscription publique via register()),
     * et avec cascade = REMOVE / ON DELETE CASCADE, ce qui aurait supprimé en
     * cascade tous les comptes créés par un utilisateur supprimé.
     * Corrigé pour suivre le même pattern que Order.createdBy : @ManyToOne,
     * colonne dédiée "created_by", optionnel, ON DELETE SET NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt = Instant.now();


}