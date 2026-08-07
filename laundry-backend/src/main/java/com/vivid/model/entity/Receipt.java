package com.vivid.model.entity;

import com.vivid.common.pdf.DocumentReferenceGenerator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * AJOUT : reçu de vente d'une commande.
 * <p>
 * Avant cet ajout, le numéro affiché sur le reçu (voir
 * {@link DocumentReferenceGenerator#generateReceiptReference})
 * était re-tiré aléatoirement à CHAQUE téléchargement : deux téléchargements
 * du même reçu produisaient deux numéros de facture différents, ce qui n'a
 * aucun sens pour un document comptable (le numéro de facture doit être
 * stable et unique par commande). De plus, rien ne "générait" jamais
 * réellement le reçu : il fallait qu'un utilisateur clique sur le bouton de
 * téléchargement pour qu'il existe.
 * <p>
 * Cette entité persiste UNE fois pour toutes la référence du reçu d'une
 * commande, créée automatiquement dès que la commande passe à
 * paymentStatus=COMPLETED (voir PaymentServiceImpl#reconcileOrderPaymentStatus),
 * puis systématiquement réutilisée par TicketServiceImpl#generateReceiptPdf
 * (le contenu du PDF reste, lui, généré à la volée à partir des données
 * actuelles de la commande — seul le numéro de référence est stable).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "receipts")
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @NotNull
    @Size(max = 50)
    @Column(name = "reference", nullable = false, length = 50, unique = true)
    private String reference;

    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "issued_at")
    private Instant issuedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "issued_by")
    private User issuedBy;
}
