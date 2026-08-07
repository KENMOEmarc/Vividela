package com.vivid.service;

import com.vivid.model.dto.PaymentDto;
import com.vivid.model.payloads.requests.PaymentRequest;

import java.util.List;

public interface PaymentService {
    /**
     * Enregistre un paiement pour une commande et notifie le client.
     * Le paiement est créé au statut PENDING (voir confirmPayment/failPayment
     * pour le faire transiter — voir revue de code, règle manquante n°4).
     *
     * @param request       informations du paiement
     * @param currentUserId utilisateur qui enregistre le paiement (peut être null)
     * @return PaymentDto créé
     */
    PaymentDto recordPayment(PaymentRequest request, Long currentUserId);

    /**
     * AJOUT : confirme un paiement PENDING (encaissement effectif) et
     * réconcilie automatiquement order.paymentStatus si la somme des
     * paiements confirmés couvre désormais le montant net dû. Voir revue de
     * code, règles manquantes n°3 et n°4.
     */
    PaymentDto confirmPayment(Long paymentId, Long currentUserId);

    /**
     * AJOUT : marque un paiement PENDING comme FAILED (échec d'encaissement,
     * ex : chèque rejeté, paiement mobile annulé). Voir revue de code, règle
     * manquante n°4.
     */
    PaymentDto failPayment(Long paymentId, Long currentUserId);

    /**
     * Liste les paiements d'une commande.
     */
    List<PaymentDto> getPaymentsByOrder(Long orderId);
}
