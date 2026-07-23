import apiClient from './authApi'

/** Récupère la liste de tous les clients. */
export const getAllCustomers = () => apiClient.get('/users/customers')

/** Récupère un client par son ID. */
export const getCustomerById = (id) => apiClient.get(`/customers/${id}`)

/** Récupère toutes les commandes d'un client. */
export const getCustomerOrders = (id) => apiClient.get(`/customers/${id}/orders`)

/** Récupère les statistiques d'un client (nombre de commandes, montant total, etc.). */
export const getCustomerStats = (id) => apiClient.get(`/customers/${id}/stats`)

/** Crée un nouveau client. */
export const createCustomer = (data) => apiClient.post('/customers', data)

/** Met à jour un client existant. */
export const updateCustomer = (id, data) => apiClient.put(`/customers/${id}`, data)

/** Supprime un client par son ID. */
export const deleteCustomer = (id) => apiClient.delete(`/customers/${id}`)
