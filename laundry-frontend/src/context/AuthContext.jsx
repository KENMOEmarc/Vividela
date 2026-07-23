/**
 * Contexte d'authentification — State management global.
 *
 * POURQUOI UN CONTEXTE ?
 *   Les informations d'authentification (token, données utilisateur, état connecté)
 *   sont nécessaires dans de nombreux composants à différents niveaux de l'arbre React.
 *   Sans contexte, il faudrait les passer en props à travers chaque composant
 *   intermédiaire ("prop drilling") → code difficile à maintenir.
 *
 *   React Context résout ce problème : un Provider au sommet de l'arbre,
 *   n'importe quel composant enfant peut consommer l'état via useContext().
 *
 * ALTERNATIVES ENVISAGEABLES :
 *   - Redux Toolkit : plus adapté pour les très grandes applications avec
 *     des états complexes et de nombreux reducers
 *   - Zustand : plus léger que Redux, bonne alternative pour les apps moyennes
 *   - React Query + Context : si beaucoup d'appels serveur avec cache
 *
 * POUR CETTE TAILLE D'APPLICATION :
 *   React Context est suffisant et évite les dépendances supplémentaires.
 *
 * PERSISTANCE :
 *   Le token et les données utilisateur sont persistés dans localStorage
 *   pour survivre aux rechargements de page (F5).
 *   À l'initialisation, le contexte relit localStorage pour restaurer l'état.
 */

import { createContext, useContext, useMemo, useReducer, useCallback } from 'react'
import { toast } from 'react-toastify'
import { loginUser, logoutUser, registerUser } from '../api/authApi'
import { STORAGE_KEYS, ROLES } from '../utils/constants'

// ── Types d'actions du reducer ─────────────────────────────────────────────

const AUTH_ACTIONS = {
  LOGIN_SUCCESS:   'LOGIN_SUCCESS',
  LOGOUT:          'LOGOUT',
  SET_LOADING:     'SET_LOADING',
  SET_ERROR:       'SET_ERROR',
  CLEAR_ERROR:     'CLEAR_ERROR'
}

// ── État initial ───────────────────────────────────────────────────────────

const getInitialState = () => {
  try {
    const token = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
    const user  = JSON.parse(localStorage.getItem(STORAGE_KEYS.USER_DATA) || 'null')
    return {
      isAuthenticated: !!token,
      user:            user,
      token:           token,
      isLoading:       false,
      error:           null
    }
  } catch {
    return { isAuthenticated: false, user: null, token: null, isLoading: false, error: null }
  }
}

// ── Reducer ────────────────────────────────────────────────────────────────

/**
 * Reducer pur : reçoit l'état actuel + une action, retourne le nouvel état.
 *
 * Pattern Reducer (inspiré de Redux) :
 *   - Prévisible : le même état + la même action → toujours le même résultat
 *   - Traçable : chaque changement d'état passe par ici
 *   - Testable : fonction pure sans effets de bord
 */
const authReducer = (state, action) => {
  switch (action.type) {

    case AUTH_ACTIONS.LOGIN_SUCCESS:
      return {
        ...state,
        isAuthenticated: true,
        user:            action.payload.user,
        token:           action.payload.token,
        isLoading:       false,
        error:           null
      }

    case AUTH_ACTIONS.LOGOUT:
      return {
        ...state,
        isAuthenticated: false,
        user:            null,
        token:           null,
        isLoading:       false,
        error:           null
      }

    case AUTH_ACTIONS.SET_LOADING:
      return { ...state, isLoading: action.payload, error: null }

    case AUTH_ACTIONS.SET_ERROR:
      return { ...state, error: action.payload, isLoading: false }

    case AUTH_ACTIONS.CLEAR_ERROR:
      return { ...state, error: null }

    default:
      return state
  }
}

// ── Création du Contexte ───────────────────────────────────────────────────

const AuthContext = createContext(null)

// ── Provider ───────────────────────────────────────────────────────────────

/**
 * AuthProvider — Enveloppe l'application et fournit l'état d'authentification.
 *
 * Doit être placé haut dans l'arbre de composants (dans App.jsx) pour que
 * tous les composants enfants puissent y accéder via useAuth().
 */
export const AuthProvider = ({ children }) => {
  const [state, dispatch] = useReducer(authReducer, undefined, getInitialState)

  /** Persiste le token et les données utilisateur dans localStorage. */
  const persistAuth = useCallback((token, user) => {
    localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, token)
    localStorage.setItem(STORAGE_KEYS.USER_DATA, JSON.stringify(user))
  }, [])

  /** Nettoie le localStorage lors de la déconnexion. */
  const clearAuth = useCallback(() => {
    localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
    localStorage.removeItem(STORAGE_KEYS.USER_DATA)
  }, [])

  /**
   * Inscription + connexion automatique.
   * @param {Object} formData - Données du formulaire d'inscription
   */
  const register = useCallback(async (formData) => {
    dispatch({ type: AUTH_ACTIONS.SET_LOADING, payload: true })
    try {
      const response = await registerUser(formData)
      const { accessToken, user } = response.data
      persistAuth(accessToken, user)
      dispatch({ type: AUTH_ACTIONS.LOGIN_SUCCESS, payload: { token: accessToken, user } })
      toast.success('Compte créé avec succès ! Bienvenue 🎉')
      return { success: true, message: response.message }
    } catch (error) {
      const message = error.response?.data?.message || 'Erreur lors de l\'inscription'
      dispatch({ type: AUTH_ACTIONS.SET_ERROR, payload: message })
      return { success: false, message }
    }
  }, [persistAuth])

  /**
   * Connexion d'un utilisateur existant.
   * @param {Object} credentials - { identifier, password }
   */
  const login = useCallback(async (credentials) => {
    dispatch({ type: AUTH_ACTIONS.SET_LOADING, payload: true })
    try {
      const response = await loginUser(credentials)
      const { accessToken, user } = response.data
      persistAuth(accessToken, user)
      dispatch({ type: AUTH_ACTIONS.LOGIN_SUCCESS, payload: { token: accessToken, user } })
      toast.success('Connexion réussie ! Bienvenue.')
      return { success: true, message: response.message }
    } catch (error) {
      const message = error.response?.data?.message || 'Identifiants invalides'
      dispatch({ type: AUTH_ACTIONS.SET_ERROR, payload: message })
      return { success: false, message }
    }
  }, [persistAuth])

  /**
   * Déconnexion : révoque le token sur le serveur + nettoie le localStorage.
   */
  const logout = useCallback(async () => {
    try {
      await logoutUser()
    } catch {
      // Même si l'appel échoue (token déjà expiré), on déconnecte localement
    } finally {
      clearAuth()
      dispatch({ type: AUTH_ACTIONS.LOGOUT })
      toast.info('Vous avez été déconnecté.')
    }
  }, [clearAuth])

  /** Efface le message d'erreur (ex: quand l'utilisateur retape). */
  const clearError = useCallback(() => {
    dispatch({ type: AUTH_ACTIONS.CLEAR_ERROR })
  }, [])

  const isAdmin    = useCallback(() => state.user?.role === ROLES.ADMIN,    [state.user])
  const isManager  = useCallback(() => state.user?.role === ROLES.MANAGER,  [state.user])
  const isEmployee = useCallback(() => state.user?.role === ROLES.EMPLOYEE, [state.user])
  const isCustomer = useCallback(() => state.user?.role === ROLES.CUSTOMER, [state.user])
  const hasRole    = useCallback((role)  => state.user?.role === role,      [state.user])
  const hasAnyRole = useCallback((roles) => roles.includes(state.user?.role), [state.user])

  /**
   * useMemo : le contexte value n'est recalculé que si state change.
   * Évite de re-rendre tous les consommateurs du contexte à chaque render du Provider.
   */
  const contextValue = useMemo(() => ({
    ...state,
    register,
    login,
    logout,
    clearError,
    isAdmin,
    isManager,
    isEmployee,
    isCustomer,
    hasRole,
    hasAnyRole,
  }), [state, register, login, logout, clearError, isAdmin, isManager, isEmployee, isCustomer, hasRole, hasAnyRole])

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  )
}

// ── Hook personnalisé ──────────────────────────────────────────────────────

/**
 * Hook useAuth — Accède au contexte d'authentification.
 *
 * USAGE dans n'importe quel composant :
 *   const { isAuthenticated, user, login, logout, error } = useAuth()
 *
 * Lève une erreur claire si utilisé en dehors du AuthProvider.
 */
export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth doit être utilisé à l\'intérieur d\'un AuthProvider')
  }
  return context
}

export default AuthContext
