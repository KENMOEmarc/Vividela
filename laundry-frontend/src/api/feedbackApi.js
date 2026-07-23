import apiClient from './authApi'

/**
 * Récupère le formulaire d'avis d'une commande (état "demandé" ou déjà
 * "soumis"). Renvoie `data: null` si la commande n'a pas encore été livrée
 * (aucun formulaire généré côté backend).
 */
export const getOrderFeedback = (orderId) => apiClient.get(`/orders/${orderId}/feedback`)

/**
 * Soumet l'avis du client sur une commande livrée.
 * @param {number|string} orderId
 * @param {{ rating: number, comment?: string }} data
 */
export const submitOrderFeedback = (orderId, data) =>
  apiClient.post(`/orders/${orderId}/feedback`, data)
