import apiClient from './authApi'

/** Récupère le stock agrégé (totaux) de tous les produits */
export const getAllStocks = () => apiClient.get('/stock')

/** Stock agrégé d'un produit spécifique, avec le détail de ses lots */
export const getStockByProduct = (productId) => apiClient.get(`/stock/product/${productId}`)

/** Liste des lots de stock d'un produit (triés par date d'expiration) */
export const getBatchesByProduct = (productId) => apiClient.get(`/stock/product/${productId}/batches`)

/** Produits en alerte (total sous le seuil) */
export const getLowStock = () => apiClient.get('/stock/low')

/** Crée un nouveau lot de stock (réapprovisionnement) */
export const createStockBatch = (data) => apiClient.post('/stock/batches', data)

/** Corrige un lot de stock existant (quantité, prix, dates) */
export const updateStockBatch = (batchId, data) => apiClient.put(`/stock/batches/${batchId}`, data)

/** Supprime un lot de stock (quantité restante nulle uniquement) */
export const deleteStockBatch = (batchId) => apiClient.delete(`/stock/batches/${batchId}`)

/** Consomme du stock pour un produit (réparti automatiquement sur les lots, FEFO) */
export const consumeStock = (data) => apiClient.post('/stock/consume', data)

/** Lots dont la date d'expiration approche */
export const getExpiringBatches = (days = 7) => apiClient.get('/stock/expiring', { params: { days } })

/** Historique des mouvements d'un produit */
export const getMovementsByProduct = (productId) => apiClient.get(`/stock/movements/${productId}`)

/** Tous les enregistrements de produits */
export const getAllRegistrations = () => apiClient.get('/stock/registrations')

/** Enregistrements d'un produit spécifique */
export const getRegistrationsByProduct = (productId) => apiClient.get(`/stock/registrations/${productId}`)
