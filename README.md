# Vividela App — Template Full-Stack

Template complet d'authentification avec **Spring Boot** (backend) et **React.js** (frontend).

## Stack technique

| Couche | Technologie | Version | Rôle |
|--------|-------------|---------|------|
| Backend | Spring Boot | 3.2.5 | Serveur REST, logique métier |
| Sécurité | Spring Security + JWT | — | Authentification stateless |
| ORM | Spring Data JPA / Hibernate | — | Mapping objet-relationnel |
| Base de données | MYSQL | 8+ | Persistance des utilisateurs |
| Cache / Session | Redis | 7+ | Blacklist JWT, cache applicatif |
| Frontend | React.js | 18.3 | Interface utilisateur SPA |
| Routage | React Router | v6 | Navigation côté client |
| Style | Tailwind CSS | 3.x | Styles utilitaires (utility-first) |
| HTTP Client | Axios | 1.7 | Appels API avec intercepteurs |
| Build | Vite | 5.x | Bundler ultra-rapide |

## Structure du monorepo

```
Templates/
├── auth-backend/          ← Projet Spring Boot (Maven)
│   ├── pom.xml
│   └── src/main/java/com/template/auth/
│       ├── config/        ← SecurityConfig, RedisConfig, CorsConfig
│       ├── controller/    ← AuthController, UserController
│       ├── model/
│       │   ├── entity/    ← User (entité JPA)
│       │   └── dto/       ← RegisterRequest, LoginRequest, AuthResponse, ApiResponse
│       ├── repository/    ← UserRepository (Spring Data JPA)
│       ├── security/      ← JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl
│       ├── service/       ← Interfaces + implémentations (impl/)
│       └── exception/     ← GlobalExceptionHandler + exceptions métier
│
├── auth-frontend/         ← Projet React (Vite + npm + Tailwind CSS)
│   ├── package.json
│   ├── vite.config.js
│   ├── tailwind.config.js
│   └── src/
│       ├── api/           ← authApi.js, userApi.js (couche HTTP Axios)
│       ├── components/
│       │   ├── auth/      ← LoginForm, RegisterForm
│       │   ├── common/    ← Navbar, Alert, Button, ActionButton, DataTable,
│       │   │                 DeleteConfirmModal, FormModal
│       │   ├── layout/    ← MainLayout, DashboardLayout, DashboardNavbar,
│       │   │                 DashboardFooter, Sidebar, SidebarData
│       │   └── users/     ← UserDataTable, UserFormModal, ConfirmDeleteModal
│       ├── context/       ← AuthContext (state global)
│       ├── hooks/         ← useAuth.js
│       ├── pages/         ← LoginPage, RegisterPage, DashboardPage, UsersPage
│       ├── types/         ← userTypes.jsx (colonnes tableau, champs formulaire, avatar)
│       ├── utils/         ← constants.js, validators.js
│       └── validations/   ← userValidations.js (validation formulaire admin)
│
└── README.md              ← Ce fichier
```

## Prérequis

- **Java 21+** — `java --version`
- **Maven 3.9+** — `mvn --version`
- **Node.js 20+** — `node --version`
- **MYSQL 8+** — Serveur démarré
- **Redis 7+** — Serveur démarré

## Démarrage rapide

### 1. Base de données MYSQL

```sql
CREATE DATABASE auth_db;
CREATE USER auth_user WITH PASSWORD 'auth_password';
GRANT ALL PRIVILEGES ON DATABASE auth_db TO auth_user;
```

### 2. Redis

```bash
# Linux/Mac
redis-server

# Windows (avec WSL ou Redis for Windows)
redis-server
```

### 3. Backend Spring Boot

```bash
cd auth-backend
mvn spring-boot:run
# → Disponible sur http://localhost:8080/api
```

### 4. Frontend React

```bash
cd auth-frontend
npm install
npm run dev
# → Disponible sur http://localhost:5173
```

## API Endpoints

### Authentification

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| POST | `/api/auth/register` | Non | Inscription (rôle CUSTOMER assigné) |
| POST | `/api/auth/login` | Non | Connexion |
| POST | `/api/auth/logout` | JWT | Déconnexion (révocation du token) |

### Utilisateurs

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| GET | `/api/users/me` | JWT | Profil de l'utilisateur connecté |
| GET | `/api/users` | JWT | Liste de tous les utilisateurs |
| POST | `/api/users` | JWT | Créer un utilisateur (admin) |
| GET | `/api/users/{id}` | JWT | Récupérer un utilisateur par ID |
| PUT | `/api/users/{id}` | JWT | Mettre à jour un utilisateur |
| DELETE | `/api/users/{id}` | JWT | Supprimer un utilisateur |

## Architecture MVC

```
┌─────────────────────────────────────────────────────────┐
│                     CLIENT (React)                      │
│  LoginForm / RegisterForm → AuthContext → authApi.js    │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP REST (JSON)
┌─────────────────────▼───────────────────────────────────┐
│                SPRING BOOT BACKEND                       │
│                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌────────────┐  │
│  │ Controller  │───▶│   Service   │───▶│ Repository │  │
│  │ (C de MVC)  │    │ (Métier)    │    │ (DAO)      │  │
│  └─────────────┘    └─────────────┘    └────────────┘  │
│         │                  │                  │         │
│  Requête/Réponse    Logique métier        JPA/SQL       │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │           Spring Security + JWT Filter          │   │
│  └─────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
    ┌─────────▼──┐   ┌───────▼──┐   ┌──────▼──────┐
    │ PostgreSQL │   │  Redis   │   │   (autres)  │
    │ (users)   │   │(blacklist)│   │             │
    └────────────┘   └──────────┘   └─────────────┘
```

## Sécurité

- Mots de passe hachés avec **BCrypt** (strength=10, jamais en clair)
- Tokens **JWT HS256** signés avec une clé secrète 256 bits
- **Blacklist Redis** pour révoquer les tokens à la déconnexion
- **CORS** configuré pour n'accepter que les origines whitelistées
- **HTTPS obligatoire** en production (le token voyage dans le header)
- Validation des données en entrée (Jakarta Validation côté backend + validators.js côté frontend)
- Messages d'erreur génériques (anti-énumération de comptes)


## Captures d'écran

#### Dashboards

<img width="1920" height="1080" alt="client-dashboard" src="https://github.com/user-attachments/assets/dceef7ec-e676-4610-a1aa-c4a96eb120ae" />

<img width="1920" height="1080" alt="Dashboard-manager" src="https://github.com/user-attachments/assets/76c96e14-5080-4ee8-a8a8-c57945ee3fe8" />

<img width="1920" height="1080" alt="employe" src="https://github.com/user-attachments/assets/466d07d2-5203-406b-9978-f8c01a119d45" />

<img width="1920" height="1080" alt="employe-com" src="https://github.com/user-attachments/assets/c21760cb-8cda-4773-8752-850d7ef83225" />

#### Services and prices

<img width="1920" height="1080" alt="services-prix" src="https://github.com/user-attachments/assets/54fb96da-e548-4439-9e39-fc1fef259802" />

<img width="1920" height="1080" alt="services" src="https://github.com/user-attachments/assets/f9178dc1-aaf1-4e15-ab1d-a02390201dab" />



#### stock

<img width="1920" height="1080" alt="stock" src="https://github.com/user-attachments/assets/3fbb79c1-d3f3-4f7d-b591-a5ea8d9f03f1" />

<img width="1920" height="1080" alt="Screenshot from 2026-07-16 10-48-25" src="https://github.com/user-attachments/assets/f28b8681-1d99-4eef-9e20-3b61cb6d95d5" />
