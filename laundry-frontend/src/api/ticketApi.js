import apiClient from './authApi'

/** Crée (ou renvoie s'il existe déjà) le ticket de dépôt d'une commande. */
export const generateTicket = (orderId) => apiClient.post(`/orders/${orderId}/ticket`)

/** Récupère les informations du ticket existant d'une commande. */
export const getTicket = (orderId) => apiClient.get(`/orders/${orderId}/ticket`)

/** Télécharge le PDF du ticket de dépôt d'une commande (créé s'il n'existe pas encore). */
export const downloadTicketPdf = (orderId) =>
  apiClient.get(`/orders/${orderId}/ticket/pdf`, { responseType: 'blob' })

/** Télécharge le PDF du reçu d'une commande. */
export const downloadReceiptPdf = (orderId) =>
  apiClient.get(`/orders/${orderId}/ticket/receipt`, { responseType: 'blob' })
