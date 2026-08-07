package com.vivid.model.entity;

import com.vivid.model.enums.TicketStatus;
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
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Size(max = 50)
    @Column(name = "barcode", length = 50)
    private String barcode;

    @Size(max = 500)
    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'GENERATED'")
    @Column(name = "status", nullable = false)
    private TicketStatus status;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "issued_at")
    private Instant issuedAt;

    /**
     * AJOUT : date/heure à partir de laquelle le ticket doit être considéré
     * comme expiré s'il n'a pas été utilisé (voir revue de code — règle
     * manquante n°16 : "Un ticket ne peut jamais expirer"). Calculée à
     * l'émission à partir de {@code vividela.ticket.validity-days} (voir
     * TicketProperties) et vérifiée paresseusement par TicketServiceImpl à
     * chaque accès au ticket.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * AJOUT : date/heure à laquelle le ticket a été téléchargé/consulté pour
     * la première fois (bascule GENERATED → DOWNLOADED). Permet de
     * distinguer un ticket jamais réclamé d'un ticket récemment consulté.
     */
    @Column(name = "downloaded_at")
    private Instant downloadedAt;

}