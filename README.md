# Vividela App — Template Full-Stack

Template complet d'authentification avec **Spring Boot** (backend) et **React.js** (frontend).

![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot%203.2.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React 18.3](https://img.shields.io/badge/React%2018.3-61DAFB?style=flat-square&logo=react&logoColor=black)
![MySQL 8+](https://img.shields.io/badge/MySQL%208%2B-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Redis 7+](https://img.shields.io/badge/Redis%207%2B-DC382D?style=flat-square&logo=redis&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![Tailwind CSS 3.x](https://img.shields.io/badge/Tailwind%20CSS%203.x-38B2AC?style=flat-square&logo=tailwindcss&logoColor=white)
![Vite 5.x](https://img.shields.io/badge/Vite%205.x-646CFF?style=flat-square&logo=vite&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![React Router v6](https://img.shields.io/badge/React%20Router%20v6-CA4245?style=flat-square&logo=reactrouter&logoColor=white)
![Axios 1.7](https://img.shields.io/badge/Axios%201.7-5A29E4?style=flat-square&logo=axios&logoColor=white)

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

<img width="1920" height="1080" alt="Bertrand-vivid" src="https://github.com/user-attachments/assets/a58ba045-0cb0-4811-a72e-b82c2463e3c3" />

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

#### Autres

<img width="653" height="1280" alt="photo_2026-07-28_06-23-36" src="https://github.com/user-attachments/assets/86e5c135-1492-4ff2-9eaf-3d50fdf613b6" />
<img width="653" height="1280" alt="photo_2026-07-28_06-21-38" src="https://github.com/user-attachments/assets/f3abc5d2-3091-45bc-9f1a-8968859c2b78" />
<img width="653" height="1280" alt="photo_2026-07-28_06-21-37" src="https://github.com/user-attachments/assets/ab2a6fb9-7a16-446f-88c2-03eda8e88202" />
<img width="1920" height="1080" alt="update-prices-services" src="https://github.com/user-attachments/assets/01b897d2-3a81-4611-aec2-e076292f7529" />
<img width="1920" height="1080" alt="tickets-recus" src="https://github.com/user-attachments/assets/67b08406-2648-4d45-9028-7b0b877ef182" />
<img width="1920" height="1080" alt="stock-alert" src="https://github.com/user-attachments/assets/9b0ae478-a1f0-4d7d-8c36-e299b23c3a13" />
<img width="1920" height="1080" alt="stock" src="https://github.com/user-attachments/assets/1402485d-a7e3-4a32-b606-91d46d63a905" />
<img width="1920" height="1080" alt="sortie-stock" src="https://github.com/user-attachments/assets/de81d3b2-3fce-4835-9db9-0366d6297ff7" />
<img width="1920" height="1080" alt="services-prix" src="https://github.com/user-attachments/assets/e62f391a-3a1e-4a85-99de-bd58f34be6fe" />
<img width="653" height="1280" alt="photo_2026-07-28_06-21-38" src="https://github.com/user-attachments/assets/671df7b4-332d-4633-a6bb-0b7c3404d6a0" />
<img width="1920" height="1080" alt="nouveau-stock" src="https://github.com/user-attachments/assets/d723779b-2b1f-437b-8619-517e4a67311a" />
<img width="1920" height="1080" alt="module-stock" src="https://github.com/user-attachments/assets/073ad2c0-caf5-439d-be7a-c6e1c19356b1" />
<img width="1920" height="1080" alt="Manager-users" src="https://github.com/user-attachments/assets/77b2859a-c6f1-4a71-8aa2-c864fcc95045" />
<img width="1920" height="1080" alt="manager-orders" src="https://github.com/user-attachments/assets/d3ef991b-ab86-458e-aed2-8d7fd85cba37" />
<img width="640" height="740" alt="manager-notif" src="https://github.com/user-attachments/assets/13649750-61c5-464d-ad42-82b388185c1f" />
<img width="1920" height="1080" alt="Luc-vetements" src="https://github.com/user-attachments/assets/2bc9c29a-90f6-4692-a191-b5a8ce9dd501" />
<img width="1920" height="1080" alt="Luc-notifs" src="https://github.com/user-attachments/assets/6a22aeff-058b-4de1-9c13-39e211196217" />
<img width="1920" height="1080" alt="Luc-Dash" src="https://github.com/user-attachments/assets/0c396399-10b1-426e-8ed3-466f6fb85e57" />
<img width="1920" height="1080" alt="lot-stock" src="https://github.com/user-attachments/assets/83f588b1-4ae6-404a-9fbf-884c9a872da0" />
<img width="1920" height="1080" alt="Employe-stock" src="https://github.com/user-attachments/assets/32495f94-101b-4e2c-864d-29b1e6256ecf" />
<img width="1920" height="1080" alt="dash-admin" src="https://github.com/user-attachments/assets/df2966f3-77d8-4e32-bedb-f99b528c022e" />
<img width="1920" height="1080" alt="Bertrand-vivid" src="https://github.com/user-attachments/assets/ab8dcdb0-fde4-4f6b-ac3d-03cb112150ff" />
<img width="1920" height="1080" alt="bertrand-notifs" src="https://github.com/user-attachments/assets/6bab186d-dc06-4e78-a76e-ea9dc5c4d7e0" />
<img width="1920" height="1080" alt="batch-clothes" src="https://github.com/user-attachments/assets/798c7421-722c-451a-b2c4-3b8ccc536fcb" />
<img width="1920" height="1080" alt="alerte-stock" src="https://github.com/user-attachments/assets/04fbe69b-b6df-40c3-99e3-314250b16e31" />
<img width="1920" height="1080" alt="Ajout-lot" src="https://github.com/user-attachments/assets/5405d330-dcc6-4a29-93f6-f33e53ba0995" />
<img width="1920" height="1080" alt="admin-order-filter" src="https://github.com/user-attachments/assets/1131ce9e-0853-4c99-bb5f-b937ace4e71e" />
<img width="1920" height="1080" alt="admin-filter" src="https://github.com/user-attachments/assets/9c605d4a-9bc4-4b49-b333-e93c16b67fec" />


