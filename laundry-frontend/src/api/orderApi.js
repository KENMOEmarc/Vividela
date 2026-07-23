import apiClient from './authApi'

/** Récupère la liste de toutes les commandes. */
export const getAllOrders = () => apiClient.get('/orders')

/**
 * Récupère les commandes filtrées par statut(s).
 * @param {string[]} statuses - ex: ['PENDING', 'IN_PROGRESS']. Si vide/absent → toutes les commandes.
 */
export const getOrdersByStatus = (statuses = []) =>
  apiClient.get('/orders', {
    params: statuses.length ? { status: statuses.join(',') } : {},
  })

/** Récupère une commande par son ID. */
export const getOrderById = (id) => apiClient.get(`/orders/${id}`)

/** Récupère une commande avec tous ses articles. */
export const getOrderWithItems = (id) => apiClient.get(`/orders/${id}/items`)

/** Récupère tous les articles d'une commande. */
export const getOrderArticles = (orderId) => apiClient.get(`/orders/${orderId}/articles`)

/** Récupère toutes les commandes d'un client. */
export const getCustomerOrders = (customerId) => apiClient.get(`/customers/${customerId}/orders`)

/** Crée une nouvelle commande. */
export const createOrder = (data) => apiClient.post('/orders', data)

/** Met à jour une commande existante. */
export const updateOrder = (id, data) => apiClient.put(`/orders/${id}`, data)

/** Supprime une commande par son ID. */
export const deleteOrder = (id) => apiClient.delete(`/orders/${id}`)

/** Ajoute un article à une commande. */
export const addOrderItem = (orderId, data) => apiClient.post(`/orders/${orderId}/items`, data)

/** Supprime un article d'une commande. */
export const removeOrderItem = (orderId, itemId) => apiClient.delete(`/orders/${orderId}/items/${itemId}`)

/** Met à jour un article d'une commande. */
export const updateOrderItem = (orderId, itemId, data) => apiClient.put(`/orders/${orderId}/items/${itemId}`, data)

/**
 * AJOUT : annule une commande, y compris AVANT qu'elle n'atteigne le stade
 * READY (le cas le plus courant en boutique). updateOrder() ne permet cette
 * transition qu'à partir de READY ; cette action dédiée couvre les stades
 * antérieurs. Voir CORRECTIONS-APPLIQUEES.md, section 1.
 */
export const cancelOrder = (id) => apiClient.patch(`/orders/${id}/cancel`)

/**
 * AJOUT : dépôt atomique — crée la commande et tous ses articles en une
 * seule transaction. `data` correspond à DepositRequest côté backend :
 * { clientId, depositDate, expectedDeliveryDate, note, articles: [...] }.
 */
export const createDeposit = (data) => apiClient.post('/orders/deposit', data)

/** AJOUT : retrouve une commande à partir du code-barres imprimé sur son ticket. */
export const findOrderByBarcode = (barcode) => apiClient.post('/orders/search-by-barcode', { barcode })

