import apiClient from './authApi'

/**
 * API des tarifs de service (prix par type de vêtement + type de service).
 * Ajout/modification réservés à ADMIN et MANAGER ; suppression réservée à ADMIN.
 */

/** Récupère la liste de tous les tarifs de service. */
export const getAllServicePrices = () => apiClient.get('/service-prices')

/** Récupère un tarif de service par son ID. */
export const getServicePriceById = (id) => apiClient.get(`/service-prices/${id}`)

/** Crée un nouveau tarif de service. */
export const createServicePrice = (data) => apiClient.post('/service-prices', data)

/** Met à jour un tarif de service existant. */
export const updateServicePrice = (id, data) => apiClient.put(`/service-prices/${id}`, data)

/** Supprime un tarif de service par son ID. */
export const deleteServicePrice = (id) => apiClient.delete(`/service-prices/${id}`)
