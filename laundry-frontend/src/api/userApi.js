import apiClient from './authApi'

export const getAllCustomers = () => apiClient.get('/users/customers')

/** Récupère la liste de tous les utilisateurs. */
export const getAllUsers = () => apiClient.get('/users')

/** Récupère un utilisateur par son ID. */
export const getUserById = (id) => apiClient.get(`/users/${id}`)

/** Crée un nouvel utilisateur (admin dashboard). */
export const createUser = (data) => apiClient.post('/users', data)

/** Met à jour un utilisateur existant (PATCH : mise à jour partielle). */
export const updateUser = (id, data) => apiClient.patch(`/users/${id}`, data)

/** Supprime un utilisateur par son ID. */
export const deleteUser = (id) => apiClient.delete(`/users/${id}`)
