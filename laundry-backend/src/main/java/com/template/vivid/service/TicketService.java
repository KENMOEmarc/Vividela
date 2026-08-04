package com.template.vivid.service;

import com.template.vivid.model.dto.GeneratedPdfDto;
import com.template.vivid.model.dto.TicketDto;
import com.template.vivid.model.entity.Order;
import com.template.vivid.model.entity.Ticket;

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
    String ensureReceiptForOrder(com.template.vivid.model.entity.Order order, String issuerEmail);
}
