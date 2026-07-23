import apiClient from './authApi'

/** Récupère la liste de tous les articles. */
export const getAllArticles = () => apiClient.get('/articles')

/** Récupère un article par son ID. */
export const getArticleById = (id) => apiClient.get(`/articles/${id}`)

/** Crée un nouveau article. */
export const createArticle = (data) => apiClient.post('/articles', data)

/** Met à jour un article existant (PATCH : mise à jour partielle). */
export const updateArticle = (id, data) => apiClient.patch(`/articles/${id}`, data)

/** Supprime un article par son ID. */
export const deleteArticle = (id) => apiClient.delete(`/articles/${id}`)
