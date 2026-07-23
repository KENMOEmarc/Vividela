import apiClient from './authApi'

/**
 * API des paiements — alignée sur PaymentController.java.
 *
 * AJOUT : ce fichier n'existait pas côté frontend alors que le backend
 * expose déjà tout le cycle de vie d'un paiement. Sans lui, aucune commande
 * ne pouvait jamais atteindre paymentStatus=COMPLETED (désormais réservé à
 * la réconciliation automatique de PaymentServiceImpl), ce qui bloquait
 * aussi silencieusement la génération du reçu (qui exige ce statut).
 */

/** Enregistre un paiement (PENDING) pour une commande. */
export const recordPayment = (data) => apiClient.post('/payments', data)

/**
 * Confirme l'encaissement effectif d'un paiement PENDING (PENDING → COMPLETED).
 * Dès que la somme des paiements confirmés couvre le montant net dû de la
 * commande, celle-ci passe automatiquement à paymentStatus=COMPLETED.
 */
export const confirmPayment = (id) => apiClient.patch(`/payments/${id}/confirm`)

/** Marque un paiement PENDING comme échoué (PENDING → FAILED). */
export const failPayment = (id) => apiClient.patch(`/payments/${id}/fail`)

/** Liste les paiements enregistrés pour une commande. */
export const getPaymentsByOrder = (orderId) => apiClient.get(`/payments/order/${orderId}`)
