package com.template.auth.model.entity;

import com.template.auth.model.enums.FeedbackSentiment;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Formulaire d'avis client rattaché à une commande livrée.
 * <p>
 * Cycle de vie :
 *   1. Dès qu'une commande passe au statut DELIVERED, une ligne est créée
 *      ici (voir FeedbackServiceImpl#requestFeedback), avec uniquement
 *      {@code requestedAt} renseigné : c'est le "formulaire envoyé" au
 *      client (matérialisé côté client par une notification + un bouton
 *      "Donner mon avis" dans son espace).
 *   2. Lorsque le client remplit et soumet le formulaire (note + commentaire
 *      facultatif), {@code rating}/{@code comment}/{@code submittedAt} sont
 *      renseignés, et l'avis est analysé par l'IA (Gemini, via Spring AI)
 *      pour en déduire {@code sentiment} et {@code aiSummary} — voir
 *      FeedbackServiceImpl#submitFeedback.
 * <p>
 * Une commande n'a jamais plus d'une ligne Feedback (contrainte d'unicité
 * sur order_id) : le formulaire n'est envoyé qu'une seule fois, et ne peut
 * être rempli qu'une seule fois.
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "order_feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /** Note donnée par le client, de 1 (très mauvais) à 5 (excellent). */
    @Column(name = "rating")
    private Integer rating;

    @Size(max = 2000)
    @Column(name = "comment", length = 2000)
    private String comment;

    /** Sentiment déduit du commentaire (et de la note) par l'analyse IA. */
    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", length = 20)
    private FeedbackSentiment sentiment;

    /** Résumé en une phrase généré par l'IA à partir de l'avis du client. */
    @Size(max = 500)
    @Column(name = "ai_summary", length = 500)
    private String aiSummary;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    /** {@code null} tant que le client n'a pas encore soumis son avis. */
    @Column(name = "submitted_at")
    private Instant submittedAt;
}
