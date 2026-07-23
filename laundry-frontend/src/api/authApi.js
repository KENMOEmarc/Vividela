/**
 * Couche API — Communication avec le backend Spring Boot.
 *
 * ARCHITECTURE DE LA COUCHE API :
 *   Tous les appels HTTP sont centralisés ici.
 *   Les composants React ne font JAMAIS d'appels fetch/axios directement.
 *   → Séparation des préoccupations (Separation of Concerns)
 *   → Un seul endroit à modifier si l'URL ou le format change
 *   → Facilite les tests (mock de cette couche uniquement)
 *
 * AXIOS :
 *   Bibliothèque HTTP qui améliore fetch() avec :
 *   - Transformation JSON automatique (pas de .json())
 *   - Intercepteurs de requête/réponse (injection de token, gestion d'erreurs globale)
 *   - Annulation de requêtes (AbortController)
 *   - Support des timeouts
 *
 * INSTANCE AXIOS PERSONNALISÉE :
 *   On crée une instance avec la baseURL et le timeout configurés.
 *   Les intercepteurs ajoutent automatiquement le token JWT à chaque requête.
 */

import axios from 'axios'
import { toast } from 'react-toastify'
import { API_BASE_URL, STORAGE_KEYS } from '../utils/constants'

// ── Instance Axios configurée ──────────────────────────────────────────────

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,                          // 10 secondes max
  headers: {
    'Content-Type': 'application/json',
    'Accept':       'application/json'
  }
})

// ── Intercepteur de REQUÊTE : injecte le token JWT ────────────────────────

/**
 * Avant chaque requête HTTP, ajoute le token Bearer dans le header Authorization.
 * L'utilisateur n'a pas besoin de le passer manuellement à chaque appel API.
 */
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Intercepteur de RÉPONSE : gestion centralisée des erreurs ─────────────

/**
 * Intercepte toutes les réponses HTTP.
 *
 * Succès (2xx) → retourne la réponse telle quelle.
 *
 * Erreur 401 (Unauthorized) → le token est expiré ou invalide :
 *   Nettoie le stockage local et redirige vers /login.
 *   Évite de devoir gérer ça dans chaque composant.
 *
 * EXCEPTION : les requêtes vers /auth/login et /auth/register renvoient
 *   aussi un 401 en cas de mauvais identifiants (voir GlobalExceptionHandler,
 *   InvalidCredentialsException → 401). Ce n'est PAS une session expirée :
 *   on ne doit ni afficher le toast "session expirée", ni rediriger/recharger
 *   la page (ce qui couperait le formulaire de connexion en cours de saisie).
 *   On laisse le composant appelant (LoginPage) afficher le message d'erreur
 *   retourné par l'API.
 *
 * Autres erreurs → relance l'erreur pour que le code appelant la gère.
 */
const AUTH_ENDPOINTS_EXCLUDED_FROM_AUTO_LOGOUT = ['/auth/login', '/auth/register']

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url || ''
    const isAuthEndpoint = AUTH_ENDPOINTS_EXCLUDED_FROM_AUTO_LOGOUT.some((endpoint) =>
      requestUrl.includes(endpoint)
    )

    if (error.response?.status === 401 && !isAuthEndpoint) {
      localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
      localStorage.removeItem(STORAGE_KEYS.USER_DATA)
      toast.warning('Votre session a expiré. Veuillez vous reconnecter.')
      // Redirige vers la page de connexion sans recharger l'application
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// ── Fonctions API ──────────────────────────────────────────────────────────

/**
 * Inscrit un nouvel utilisateur.
 *
 * @param {Object} data - { username, email, phone, password, confirmPassword, firstName, lastName }
 * @returns {Promise<{data: {accessToken, tokenType, expiresIn, user}}>}
 */
export const registerUser = async (data) => {
  const response = await apiClient.post('/auth/register', data)
  return response.data   // ApiResponse<AuthResponse>
}

/**
 * Connecte un utilisateur existant.
 *
 * @param {Object} data - { identifier, password }  (identifier = email ou username)
 * @returns {Promise<{data: {accessToken, tokenType, expiresIn, user}}>}
 */
export const loginUser = async (data) => {
  const response = await apiClient.post('/auth/login', data)
  return response.data   // ApiResponse<AuthResponse>
}

/**
 * Déconnecte l'utilisateur courant (révoque le token JWT côté serveur).
 *
 * Le token est automatiquement injecté par l'intercepteur de requête.
 */
export const logoutUser = async () => {
  const response = await apiClient.post('/auth/logout')
  return response.data
}

/**
 * Récupère le profil de l'utilisateur connecté.
 * Nécessite un token JWT valide (injecté automatiquement).
 *
 * @returns {Promise<{data: {id, userName, email, phone, firstName, lastName}}>}
 */
export const getCurrentUser = async () => {
  const response = await apiClient.get('/users/me')
  return response.data   // ApiResponse<UserInfo>
}

export default apiClient
