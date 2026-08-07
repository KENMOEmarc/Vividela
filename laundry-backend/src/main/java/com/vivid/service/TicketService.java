package com.vivid.service;

import com.vivid.model.dto.GeneratedPdfDto;
import com.vivid.model.dto.TicketDto;
import com.vivid.model.entity.Order;
import com.vivid.model.entity.Ticket;

public interface TicketService {

    /**
     * Crée (ou renvoie, si déjà existant) le ticket associé à une commande.
     * La commande doit posséder au moins un article (vêtement) enregistré.
     */
    TicketDto generateTicket(Long orderId);

    /**
     * Crée immédiatement le ticket (et son code-barre) pour une commande qui vient
     * d'être créée, ou renvoie le ticket existant s'il y en a déjà un.
     * Contrairement à {@link #generateTicket(Long)}, cette méthode NE nécessite PAS
     * que des articles soient déjà enregistrés : le code-barre identifie la commande
     * dès sa création et sert de référence pour le dépôt des vêtements.
     */
    Ticket ensureTicketForOrder(Order order);

    /**
     * Génère le PDF du ticket pour une commande et renvoie son contenu binaire.
     * Crée le ticket s'il n'existe pas encore.
     */
    GeneratedPdfDto generateTicketPdf(Long orderId);

    TicketDto getTicketByOrderId(Long orderId);

    /**
     * Génère le PDF du reçu pour une commande et renvoie son contenu binaire.
     * La commande doit posséder au moins un article (vêtement) enregistré.
     *
     * @param orderId     identifiant de la commande facturée
     * @param issuerEmail e-mail de l'utilisateur actuellement authentifié qui
     *                    demande la génération du reçu (affiché sur le document
     *                    comme "Émis par"). Peut être {@code null} (ex. génération
     *                    automatique sans contexte utilisateur).
     */
    GeneratedPdfDto generateReceiptPdf(Long orderId, String issuerEmail);

    /**
     * AJOUT : crée (de façon idempotente) et persiste la référence du reçu
     * d'une commande dès qu'elle est intégralement payée — voir revue de
     * code : "Aucun reçu n'est jamais réellement généré après un paiement,
     * et le numéro de reçu n'est pas stable d'un téléchargement à l'autre".
     * Appelée automatiquement par PaymentServiceImpl juste après qu'une
     * commande bascule à paymentStatus=COMPLETED ; peut aussi être appelée
     * paresseusement au premier téléchargement du reçu si, pour une raison
     * quelconque, elle n'avait pas encore été invoquée.
     * <p>
     * Si un reçu existe déjà pour cette commande, sa référence existante est
     * simplement renvoyée (aucun doublon n'est créé).
     *
     * @param order       la commande intégralement payée
     * @param issuerEmail e-mail de l'utilisateur à l'origine de la génération
     *                    (peut être {@code null} pour une génération automatique
     *                    sans contexte utilisateur, ex. déclenchée par le
     *                    système lors de la réconciliation des paiements)
     * @return la référence stable du reçu (existante ou nouvellement créée)
     */
    String ensureReceiptForOrder(Order order, String issuerEmail);
}
