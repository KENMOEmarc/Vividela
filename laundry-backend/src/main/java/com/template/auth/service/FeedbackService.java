package com.template.auth.service;

import com.template.auth.model.dto.FeedbackDto;
import com.template.auth.model.dto.FeedbackSubmitRequest;
import com.template.auth.model.entity.Order;

public interface FeedbackService {

    /**
     * Génère (de façon idempotente) le formulaire d'avis d'une commande qui
     * vient de passer au statut DELIVERED, et notifie le client qu'il peut
     * le remplir depuis son espace. Appelé par OrderServiceImpl#updateOrder
     * juste après la persistance de la transition vers DELIVERED.
     */
    void requestFeedback(Order order);

    /**
     * Récupère le formulaire d'avis d'une commande (état "demandé" ou déjà
     * "soumis"), pour affichage côté client (bouton "Donner mon avis") ou
     * côté personnel. Retourne {@code null} si la commande n'a pas encore
     * été livrée (aucun formulaire généré).
     *
     * @param isStaff true si l'appelant est ADMIN/MANAGER/EMPLOYEE (accès à
     *                n'importe quelle commande) ; sinon l'appelant doit être
     *                le client propriétaire de la commande.
     */
    FeedbackDto getFeedbackForOrder(Long orderId, Long callerId, boolean isStaff);

    /**
     * Soumission de l'avis par le client. Déclenche l'analyse IA (Gemini)
     * du commentaire, puis notifie le manager du pressing (et l'admin, en
     * plus, si l'avis est jugé négatif).
     */
    FeedbackDto submitFeedback(Long orderId, Long customerId, FeedbackSubmitRequest request);
}
