# Vividela-frontend — Documentation Technique

Frontend React.js pour l'authentification et la gestion des utilisateurs. SPA (Single Page Application) avec Tailwind CSS, React Router v6, Axios et Context API.

---

## Table des matières

1. [Architecture](#1-architecture)
2. [Structure des fichiers](#2-structure-des-fichiers)
3. [Flux de données](#3-flux-de-données)
4. [Couche API (Axios)](#4-couche-api-axios)
5. [Context d'authentification](#5-context-dauthentification)
6. [Composants](#6-composants)
7. [Pages et routage](#7-pages-et-routage)
8. [Validation](#8-validation)
9. [Variables d'environnement](#9-variables-denvironnement)
10. [Scripts disponibles](#10-scripts-disponibles)

---

## 1. Architecture

### Diagramme de flux

```
┌─────────────────────────────────────────────────────────┐
│                    NAVIGATEUR                           │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │              React Router v6                    │   │
│  │  /login → MainLayout → LoginPage               │   │
│  │  /register → MainLayout → RegisterPage         │   │
│  │  /dashboard → DashboardLayout → DashboardPage  │   │
│  │  /dashboard/users → DashboardLayout → UsersPage│   │
│  └────────────────────┬────────────────────────────┘   │
│                       │                                 │
│  ┌────────────────────▼────────────────────────────┐   │
│  │  AuthContext (useReducer + localStorage)        │   │
│  │  État global : isAuthenticated, user, token     │   │
│  │  Actions : login(), register(), logout()        │   │
│  └────────────────────┬────────────────────────────┘   │
│                       │                                 │
│  ┌────────────────────▼────────────────────────────┐   │
│  │  authApi.js / userApi.js — Couche HTTP (Axios)  │   │
│  │  Instance configurée : baseURL + timeout        │   │
│  │  Intercepteur requête : injecte Bearer token    │   │
│  │  Intercepteur réponse : gère 401 (token expiré)│   │
│  └────────────────────┬────────────────────────────┘   │
└───────────────────────┼─────────────────────────────────┘
                        │ HTTP REST
            ┌───────────▼───────────┐
            │  Spring Boot Backend  │
            │  :8080/api            │
            └───────────────────────┘
```

---

## 2. Structure des fichiers

```
src/
│
├── main.jsx                    ← Point d'entrée React (createRoot)
│
├── App.jsx                     ← Routage React Router v6
│                                 ProtectedRoute, RoleProtectedRoute, HomeRedirect
│
├── index.css                   ← Styles globaux (directives Tailwind)
│
├── api/
│   ├── authApi.js              ← Instance Axios + intercepteurs + fonctions d'authentification
│   │                             registerUser(), loginUser(), logoutUser(), getCurrentUser()
│   └── userApi.js              ← Fonctions CRUD utilisateurs (admin)
│                                 getAllUsers(), createUser(), updateUser(), deleteUser()
│
├── context/
│   └── AuthContext.jsx         ← createContext + useReducer + Provider + hook useAuth
│                                 Persistance localStorage (token + user)
│
├── hooks/
│   └── useAuth.js              ← Re-export du hook useAuth depuis AuthContext
│
├── components/
│   ├── auth/
│   │   ├── LoginForm.jsx       ← Formulaire connexion (identifiant + mot de passe)
│   │   └── RegisterForm.jsx    ← Formulaire inscription (6 champs + validation temps réel)
│   │
│   ├── common/
│   │   ├── Navbar.jsx          ← Barre de navigation publique (Tailwind, responsive)
│   │   ├── Alert.jsx           ← Alerte réutilisable (success/danger/warning/info)
│   │   ├── Button.jsx          ← Bouton générique (variantes : primary/secondary/danger)
│   │   ├── ActionButton.jsx    ← Bouton d'action icône pour tableaux (éditer/supprimer)
│   │   ├── DataTable.jsx       ← Tableau générique avec tri, pagination, recherche
│   │   ├── DeleteConfirmModal.jsx ← Modale de confirmation de suppression (générique)
│   │   └── FormModal.jsx       ← Modale formulaire générique (création/édition)
│   │
│   ├── layout/
│   │   ├── MainLayout.jsx      ← Layout public : Navbar + <Outlet /> + Footer
│   │   ├── DashboardLayout.jsx ← Layout protégé : Sidebar + DashboardNavbar + <Outlet />
│   │   ├── DashboardNavbar.jsx ← Barre de navigation du dashboard (breadcrumb, profil)
│   │   ├── DashboardFooter.jsx ← Pied de page du dashboard
│   │   ├── Sidebar.jsx         ← Menu latéral avec navigation (liens selon rôle)
│   │   └── SidebarData.js      ← Configuration des entrées du menu (icônes, routes, rôles)
│   │
│   └── users/
│       ├── UserDataTable.jsx   ← Tableau des utilisateurs (colonnes définies dans userTypes)
│       ├── UserFormModal.jsx   ← Modale création/édition d'un utilisateur
│       └── ConfirmDeleteModal.jsx ← Modale suppression utilisateur (wrapper de DeleteConfirmModal)
│
├── pages/
│   ├── LoginPage.jsx           ← Page connexion (redirection si déjà connecté)
│   ├── RegisterPage.jsx        ← Page inscription (redirection si déjà connecté)
│   ├── DashboardPage.jsx       ← Tableau de bord (protégé)
│   └── UsersPage.jsx           ← Gestion des utilisateurs (ADMIN/EMPLOYEE uniquement)
│
├── types/
│   └── userTypes.jsx           ← Définitions UI liées aux utilisateurs :
│                                 USER_TABLE_COLUMNS, getUserFormFields(),
│                                 getUserInitials(), getUserAvatarGradient(),
│                                 USER_ROLE_BADGE, renderUserDeletePreview()
│
├── utils/
│   ├── constants.js            ← API_BASE_URL, STORAGE_KEYS, ROUTES, ROLES, ROLE_LABELS,
│   │                             PASSWORD_MIN_LENGTH, PASSWORD_REGEX, USERNAME_REGEX
│   └── validators.js           ← Validation formulaires d'authentification
│                                 validateRegisterForm(), validateLoginForm() + helpers
│
└── validations/
    └── userValidations.js      ← Validation formulaire admin utilisateur
                                  validateUserField(), validateUserForm()
```

---

## 3. Flux de données

### 3.1 Inscription

```
RegisterForm (state: formData, fieldErrors)
  │ onChange → handleChange (mise à jour champ + nettoyage erreur)
  │ onBlur  → handleBlur (validation partielle du champ perdu)
  │ onSubmit → handleSubmit
  │
  ├── validateRegisterForm(formData) → { champ: "message" }
  │   Erreurs → setFieldErrors → affichage bordure rouge + message
  │
  └── Valide → AuthContext.register(formData)
        │
        ├── dispatch SET_LOADING → bouton spinner
        ├── authApi.registerUser(formData) → POST /api/auth/register
        │     Succès → { success: true, data: { accessToken, user } }
        │     Erreur → { response.data.message }
        │
        ├── Succès → persistAuth(token, user) → localStorage
        │           dispatch LOGIN_SUCCESS → isAuthenticated = true
        │           navigate('/dashboard')
        │
        └── Erreur → dispatch SET_ERROR → error affiché dans <Alert>
```

### 3.2 Connexion

```
LoginForm
  │ handleSubmit → validateLoginForm → AuthContext.login()
  │
  └── authApi.loginUser({ identifier, password })
        → POST /api/auth/login
        Succès  → token + user → localStorage → /dashboard
        401     → "Identifiants invalides" → <Alert danger>
```

### 3.3 Gestion des utilisateurs (UsersPage)

```
UsersPage (mount)
  └── userApi.getAllUsers() → GET /api/users
        → state: users[]

UserFormModal (création)
  └── userApi.createUser(data) → POST /api/users → rafraîchit la liste

UserFormModal (édition)
  └── userApi.updateUser(id, data) → PUT /api/users/{id} → rafraîchit la liste

ConfirmDeleteModal
  └── userApi.deleteUser(id) → DELETE /api/users/{id} → rafraîchit la liste
```

### 3.4 Requête protégée (automatique)

```
Axios intercepteur requête (avant chaque appel)
  └── localStorage.getItem('auth_token')
       Si token → Authorization: Bearer <token> ajouté au header

Axios intercepteur réponse (après chaque réponse)
  └── 401 Unauthorized
       → localStorage.clear()
       → window.location.href = '/login'
       (token expiré ou révoqué côté serveur)
```

---

## 4. Couche API (Axios)

### Instance configurée (authApi.js / userApi.js)

```javascript
const apiClient = axios.create({
  baseURL: '/api',        // Proxy Vite → localhost:8080/api
  timeout: 10000,         // 10 secondes max
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
})
```

### authApi.js — Fonctions exportées

| Fonction | Méthode | URL | Body |
|----------|---------|-----|------|
| `registerUser(data)` | POST | `/auth/register` | RegisterRequest |
| `loginUser(data)` | POST | `/auth/login` | LoginRequest |
| `logoutUser()` | POST | `/auth/logout` | — |
| `getCurrentUser()` | GET | `/users/me` | — |

### userApi.js — Fonctions exportées

| Fonction | Méthode | URL | Body |
|----------|---------|-----|------|
| `getAllUsers()` | GET | `/users` | — |
| `createUser(data)` | POST | `/users` | UserFormRequest |
| `updateUser(id, data)` | PUT | `/users/{id}` | UserFormRequest |
| `deleteUser(id)` | DELETE | `/users/{id}` | — |

---

## 5. Context d'authentification

### État géré (useReducer)

```javascript
state = {
  isAuthenticated: boolean,   // true si token valide présent
  user: {                     // informations de l'utilisateur connecté
    id, username, email, firstName, lastName, role
  },
  token: string | null,       // JWT Bearer token
  isLoading: boolean,         // true pendant les appels API
  error: string | null        // message d'erreur à afficher
}
```

### Actions du reducer

| Action | Déclencheur | Résultat |
|--------|-------------|---------|
| `LOGIN_SUCCESS` | login/register réussi | isAuthenticated=true, user+token remplis |
| `LOGOUT` | logout() | isAuthenticated=false, user+token null |
| `SET_LOADING` | avant appel API | isLoading=true, error=null |
| `SET_ERROR` | erreur API | error=message, isLoading=false |
| `CLEAR_ERROR` | utilisateur retape | error=null |

### Hook `useAuth()`

```javascript
// Importable depuis context/AuthContext ou hooks/useAuth.js
const {
  isAuthenticated,  // boolean
  user,             // { id, username, email, firstName, lastName, role }
  token,            // string JWT
  isLoading,        // boolean
  error,            // string | null
  register,         // async (formData) → { success, message }
  login,            // async (credentials) → { success, message }
  logout,           // async () → void
  clearError,       // () → void
  hasAnyRole        // (roles: string[]) → boolean
} = useAuth()
```

### Persistance localStorage

| Clé | Valeur |
|-----|--------|
| `auth_token` | Token JWT (string) |
| `auth_user` | Données user (JSON stringifié) |

---

## 6. Composants

### `LoginForm`

- 2 champs : identifier (email ou username), password
- Toggle password : bouton œil pour afficher/masquer
- Focus auto au montage via `useRef + useEffect`
- Redirection post-login vers `location.state.from` ou `/dashboard`

### `RegisterForm`

- 6 champs : firstName, lastName, username, email, password, confirmPassword
- Toggle password sur les deux champs
- Validation temps réel : `onBlur` valide le champ qui perd le focus
- Feedback visuel Tailwind : bordure rouge/verte selon la validité

### `Alert`

Props : `type` ('success'|'danger'|'warning'|'info'), `message` (string), `onClose`

### `Button`

Props : `variant` ('primary'|'secondary'|'danger'), `icon`, `loading`, `disabled`, `className`

### `ActionButton`

Bouton icône compact pour les actions de tableau (éditer, supprimer). Props : `icon`, `onClick`, `variant`, `title`.

### `DataTable`

Tableau générique avec :
- Tri par colonne (click en-tête)
- Recherche texte
- Pagination
- Colonnes configurables via prop `columns` (voir `userTypes.jsx` pour exemple)

### `DeleteConfirmModal`

Modale de confirmation générique. Props : `isOpen`, `title`, `description`, `onConfirm`, `onClose`, `renderPreview`, `confirmLabel`.

### `FormModal`

Modale formulaire générique avec chargement, validation et fermeture au clic extérieur.

### `Navbar`

Barre de navigation publique (pages login/register). Responsive avec menu hamburger mobile. Affiche les liens selon `isAuthenticated`.

### `DashboardLayout` / `DashboardNavbar` / `Sidebar`

Layout protégé du dashboard :
- `Sidebar` : menu latéral configurable via `SidebarData.js` (liens filtrés par rôle)
- `DashboardNavbar` : barre du haut avec breadcrumb et menu profil
- `DashboardFooter` : pied de page du dashboard

### `UserDataTable`

Tableau des utilisateurs utilisant `DataTable` + colonnes définies dans `userTypes.jsx`. Inclut avatar, badge de rôle, statut actif/inactif.

### `UserFormModal`

Modale création/édition d'un utilisateur. Champs définis via `getUserFormFields()` depuis `userTypes.jsx`. Validation via `validateUserForm()`.

### `ConfirmDeleteModal`

Wrapper spécialisé de `DeleteConfirmModal` pour les utilisateurs. Affiche un aperçu du compte à supprimer via `renderUserDeletePreview()`.

---

## 7. Pages et routage

### Configuration des routes (`App.jsx`)

```
BrowserRouter
  └── AuthProvider
        └── Routes
              ├── Route path="/"  (MainLayout — public)
              │     ├── index → HomeRedirect (/login ou /dashboard)
              │     ├── /login → LoginPage
              │     └── /register → RegisterPage
              │
              ├── Route path="/dashboard" → ProtectedRoute → DashboardLayout
              │     ├── index → DashboardPage
              │     └── /dashboard/users → RoleProtectedRoute [ADMIN, EMPLOYEE]
              │                              → UsersPage
              └── Route path="*" → 404
```

### `ProtectedRoute`

Redirige vers `/login` si non authentifié. Mémorise l'URL cible dans `location.state.from`.

### `RoleProtectedRoute`

Redirige vers `/dashboard` si l'utilisateur n'a pas le rôle requis. Paramètre : `roles` (tableau de rôles autorisés).

---

## 8. Validation

### `utils/validators.js` — Formulaires d'authentification

| Fonction | Paramètres | Retour |
|----------|-----------|--------|
| `validateRequired(value, name)` | value: string | null ou message |
| `validateEmail(email)` | email: string | null ou message |
| `validateUsername(username)` | username: string | null ou message |
| `validatePassword(password)` | password: string | null ou message |
| `validateConfirmPassword(pwd, confirm)` | 2 strings | null ou message |
| `validateName(name, fieldName)` | name: string | null ou message |
| `validateRegisterForm(formData)` | objet complet | `{ champ: message }` |
| `validateLoginForm(formData)` | objet complet | `{ champ: message }` |

### `validations/userValidations.js` — Formulaire admin utilisateur

| Fonction | Paramètres | Retour |
|----------|-----------|--------|
| `validateUserField(key, value, { mode })` | key, value, mode ('create'|'edit') | null ou message |
| `validateUserForm(data, mode)` | objet + mode | `{ champ: message }` |

Le mode `'edit'` rend le mot de passe optionnel (vide = conserver l'actuel).

### Règles de validation communes

| Champ | Règle |
|-------|-------|
| username | 3-50 chars, `[a-zA-Z0-9_]+` uniquement |
| email | Format RFC (contient @ et domaine) |
| password | ≥8 chars, maj + min + chiffre + spécial (`@$!%*?&`) |
| confirmPassword | Identique à password |
| firstName/lastName | Non vide, max 100 chars |

---

## 9. Variables d'environnement

Créer un fichier `.env` à la racine de `auth-frontend/` (voir `.env.example`) :

```env
# URL de base de l'API backend (proxy Vite en développement)
VITE_API_URL=/api

# En production avec un vrai domaine :
# VITE_API_URL=https://api.mondomaine.com/api
```

Les variables Vite doivent commencer par `VITE_` pour être accessibles via `import.meta.env.VITE_NOM_VARIABLE`.

---

## 10. Scripts disponibles

```bash
npm run dev      # Démarre Vite en mode développement (HMR)
npm run build    # Build de production (dist/)
npm run preview  # Prévisualise le build de production localement
npm run lint     # Analyse statique ESLint
```

### Proxy de développement (vite.config.js)

```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

Tout appel à `/api/...` est redirigé vers `http://localhost:8080/api/...` — pas de problème CORS en développement.
