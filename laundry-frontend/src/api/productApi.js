import apiClient from './authApi'

/** Récupère la liste de tous les produits. */
export const getAllProducts = () => apiClient.get('/products')

/** Récupère un produit par son ID. */
export const getProductById = (id) => apiClient.get(`/products/${id}`)

/** Crée un nouveau produit. */
export const createProduct = (data) => apiClient.post('/products', data)

/** Met à jour un produit existant. */
export const updateProduct = (id, data) => apiClient.put(`/products/${id}`, data)

/** Supprime un produit par son ID. */
export const deleteProduct = (id) => apiClient.delete(`/products/${id}`)
