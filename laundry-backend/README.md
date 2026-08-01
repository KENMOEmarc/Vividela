# vividela-backend — Documentation Technique

Backend Spring Boot pour l'authentification utilisateur. Architecture MVC stricte avec JWT, PostgreSQL et Redis.

---

## Table des matières

1. [Architecture](#1-architecture)
2. [Structure des packages](#2-structure-des-packages)
3. [Flux de données](#3-flux-de-données)
4. [Modèles et DTOs](#4-modèles-et-dtos)
5. [Couche Repository](#5-couche-repository)
6. [Couche Service](#6-couche-service)
7. [Couche Controller](#7-couche-controller)
8. [Sécurité Spring Security + JWT](#8-sécurité-spring-security--jwt)
9. [Redis — Blacklist JWT](#9-redis--blacklist-jwt)
10. [Gestion des exceptions](#10-gestion-des-exceptions)
11. [Configuration](#11-configuration)
12. [Tests API (exemples curl)](#12-tests-api-exemples-curl)

---

## 1. Architecture

### Patron MVC (Model-View-Controller)

```
┌──────────────────────────────────────────────────────────┐
│  CLIENT HTTP (React / Postman / curl)                    │
└──────────────────┬───────────────────────────────────────┘
                   │ JSON sur HTTP/HTTPS
┌──────────────────▼───────────────────────────────────────┐
│  SPRING SECURITY FILTER CHAIN                            │
│  JwtAuthenticationFilter → valide le Bearer token        │
│  SecurityFilterChain → autorise ou rejette la requête    │
└──────────────────┬───────────────────────────────────────┘
                   │ Requête autorisée
┌──────────────────▼───────────────────────────────────────┐
│  CONTROLLER (Couche C — Présentation)                    │
│  AuthController, UserController                          │
│  - Désérialise le JSON entrant (@RequestBody)            │
│  - Valide les données (@Valid)                           │
│  - Délègue au Service                                    │
│  - Sérialise la réponse (ApiResponse<T>)                │
└──────────────────┬───────────────────────────────────────┘
                   │
┌──────────────────▼───────────────────────────────────────┐
│  SERVICE (Couche Métier)                                 │
│  UserServiceImpl, AuthServiceImpl                        │
│  - Logique métier : validation, orchestration            │
│  - Appelle les repositories et la sécurité               │
│  - Transactions @Transactional                           │
└──────────────────┬───────────────────────────────────────┘
                   │
┌──────────────────▼───────────────────────────────────────┐
│  REPOSITORY (Couche Accès données)                       │
│  UserRepository extends JpaRepository                    │
│  - Requêtes SQL / JPQL automatiques                      │
│  - Hibernate comme implémentation JPA                    │
└──────────────────┬───────────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
  PostgreSQL               Redis
  (tables JPA)       (blacklist JWT)
```

---

## 2. Structure des packages

```
com.template.vivid/
│
├── AuthApplication.java          ← Point d'entrée @SpringBootApplication
│
├── config/
│   ├── SecurityConfig.java       ← Chaîne de filtres Spring Security, BCrypt, AuthManager
│   ├── RedisConfig.java          ← RedisTemplate, CacheManager, sérialisation JSON
│   └── CorsConfig.java           ← Origines autorisées, méthodes, headers CORS
│
├── controller/                   ← Couche C (MVC) — Points d'entrée HTTP REST
│   ├── AuthController.java       ← POST /auth/register, /auth/login, /auth/logout
│   └── UserController.java       ← GET /users/me, GET/POST /users, GET/PUT/DELETE /users/{id}
│
├── model/                        ← Couche M (MVC) — Données
│   ├── entity/
│   │   └── User.java             ← Entité JPA ↔ table "users" PostgreSQL
│   └── dto/
│       ├── RegisterRequest.java  ← DTO entrée : données d'inscription + validations
│       ├── LoginRequest.java     ← DTO entrée : identifiant + mot de passe
│       ├── UserFormRequest.java  ← DTO entrée : création/mise à jour admin d'un utilisateur
│       ├── UserDto.java          ← DTO sortie : représentation complète d'un utilisateur
│       ├── AuthResponse.java     ← DTO sortie : token JWT + infos utilisateur
│       └── ApiResponse.java      ← Enveloppe standardisée pour toutes les réponses
│
├── repository/
│   └── UserRepository.java       ← Interface JPA avec méthodes dérivées et JPQL
│
├── security/
│   ├── JwtTokenProvider.java     ← Génération, parsing et validation des tokens JWT
│   ├── JwtAuthenticationFilter.java ← Filtre HTTP : extrait et valide le Bearer token
│   └── UserDetailsServiceImpl.java  ← Charge un User depuis la DB pour Spring Security
│
├── service/
│   ├── UserService.java          ← Interface du service utilisateur
│   ├── AuthService.java          ← Interface du service d'authentification
│   ├── TokenBlacklistService.java ← Interface de la blacklist JWT Redis
│   └── impl/
│       ├── UserServiceImpl.java       ← register(), findAll(), findById(), findByEmail(),
│       │                                 create(), update(), delete()
│       ├── AuthServiceImpl.java       ← register(), login(), logout()
│       └── TokenBlacklistServiceImpl.java ← blacklist(), isBlacklisted() via Redis
│
└── exception/
    ├── GlobalExceptionHandler.java    ← @RestControllerAdvice : intercepte toutes les exceptions
    ├── UserAlreadyExistsException.java ← 409 Conflict
    ├── InvalidCredentialsException.java ← 401 Unauthorized
    └── PasswordMismatchException.java  ← 400 Bad Request
```

---

## 3. Flux de données

### 3.1 Inscription (`POST /api/auth/register`)

```
Client
  │ POST /api/auth/register
  │ Body: { username, email, password, confirmPassword, firstName, lastName }
  ▼
JwtAuthenticationFilter
  │ Pas de token requis (route publique) → passe au suivant
  ▼
AuthController.register()
  │ @Valid RegisterRequest → validation Jakarta (annotations sur le DTO)
  │ Erreur de validation → 400 Bad Request (GlobalExceptionHandler)
  ▼
AuthServiceImpl.register()
  ├── UserServiceImpl.register()
  │     ├── existsByEmail() → 409 si email pris
  │     ├── existsByUsername() → 409 si username pris
  │     ├── password != confirmPassword → 400
  │     ├── BCrypt.encode(password) → hash
  │     └── userRepository.save(user) → INSERT PostgreSQL
  │
  └── JwtTokenProvider.generateToken() → JWT signé HS256
        │ Claims: sub=email, jti=UUID, userId, username, iat, exp
        └── Retour: "eyJhbGci..."
  ▼
AuthController
  │ 201 Created
  │ Body: ApiResponse { success: true, data: { accessToken, tokenType, expiresIn, user } }
  ▼
Client
  └── Stocke le token dans localStorage
```

### 3.2 Connexion (`POST /api/auth/login`)

```
Client
  │ POST /api/auth/login
  │ Body: { identifier: "jean@ex.com", password: "MonMdp@1" }
  ▼
AuthController.login()
  ▼
AuthServiceImpl.login()
  ├── authenticationManager.authenticate(UsernamePasswordAuthToken)
  │     ├── DaoAuthenticationProvider → UserDetailsServiceImpl.loadUserByUsername()
  │     │     └── userRepository.findByEmailOrUsername() → User depuis PostgreSQL
  │     └── BCrypt.matches(password, hash) → true/false
  │         false → BadCredentialsException → InvalidCredentialsException → 401
  │
  ├── userRepository.findByEmailOrUsername() → entité User complète
  └── JwtTokenProvider.generateToken() → JWT signé
  ▼
200 OK + token JWT
```

### 3.3 Requête authentifiée (`GET /api/users/me`)

```
Client
  │ GET /api/users/me
  │ Header: Authorization: Bearer eyJhbGci...
  ▼
JwtAuthenticationFilter
  ├── Extrait le token du header
  ├── JwtTokenProvider.validateToken() → signature OK + non expiré
  ├── TokenBlacklistService.isBlacklisted(jti) → false (pas révoqué)
  ├── extractEmail() → "jean@exemple.com"
  ├── UserDetailsServiceImpl.loadUserByUsername() → UserDetails
  └── SecurityContextHolder.setAuthentication() → requête authentifiée
  ▼
UserController.getCurrentUser()
  ├── @AuthenticationPrincipal UserDetails → email
  └── UserService.findByEmail() → User
  ▼
200 OK + UserInfo { id, username, email, firstName, lastName }
```

---

## 4. Modèles et DTOs

### Entité `User`

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| id | Long | BIGSERIAL | PK, auto-incrémenté |
| username | String | VARCHAR(50) | UNIQUE, NOT NULL, 3-50 chars |
| email | String | VARCHAR(150) | UNIQUE, NOT NULL, format email |
| password | String | VARCHAR(255) | NOT NULL (hash BCrypt) |
| firstName | String | VARCHAR(100) | NOT NULL |
| lastName | String | VARCHAR(100) | NOT NULL |
| enabled | boolean | BOOLEAN | DEFAULT true |
| createdAt | LocalDateTime | TIMESTAMP | auto (INSERT) |
| updatedAt | LocalDateTime | TIMESTAMP | auto (UPDATE) |

### DTO `RegisterRequest` — Validations Jakarta

| Champ | Contraintes |
|-------|-------------|
| username | @NotBlank, @Size(3,50), @Pattern([a-zA-Z0-9_]+) |
| email | @NotBlank, @Email, @Size(max=150) |
| password | @NotBlank, @Size(8,100), @Pattern(maj+min+chiffre+special) |
| confirmPassword | @NotBlank |
| firstName | @NotBlank, @Size(max=100) |
| lastName | @NotBlank, @Size(max=100) |

### DTO `ApiResponse<T>` — Structure unifiée

```json
{
  "success": true,
  "message": "Inscription réussie !",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "username": "jean_dupont",
      "email": "jean@exemple.com",
      "firstName": "Jean",
      "lastName": "Dupont"
    }
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## 5. Couche Repository

`UserRepository extends JpaRepository<User, Long>` :

| Méthode | Type | SQL généré |
|---------|------|------------|
| `findByEmailIgnoreCase(email)` | Dérivée | `SELECT * WHERE LOWER(email)=LOWER(?)` |
| `findByUsernameIgnoreCase(username)` | Dérivée | `SELECT * WHERE LOWER(username)=LOWER(?)` |
| `existsByEmailIgnoreCase(email)` | Dérivée | `SELECT EXISTS(...)` |
| `existsByUsernameIgnoreCase(username)` | Dérivée | `SELECT EXISTS(...)` |
| `findByEmailOrUsernameIgnoreCase(id)` | @Query JPQL | `WHERE LOWER(email)=? OR LOWER(username)=?` |

---

## 6. Couche Service

### `UserServiceImpl.register()` — Algorithme (inscription publique)

```
1. existsByEmail(email)  → UserAlreadyExistsException (409)
2. existsByUsername(username) → UserAlreadyExistsException (409)
3. password != confirmPassword → PasswordMismatchException (400)
4. BCryptPasswordEncoder.encode(password) → hash
5. User.builder()...role(CUSTOMER)...build() → entité (rôle CUSTOMER forcé)
6. userRepository.save(user) → INSERT + ID généré
7. return User persisté
```

### `UserServiceImpl.create()` — Création admin

```
1. existsByEmail() / existsByUsername() → 409 si déjà pris
2. password requis (sinon 400)
3. Role = request.role ou CUSTOMER par défaut
4. BCrypt.encode(password) → hash
5. userRepository.save() → INSERT
```

### `UserServiceImpl.update(id, request)`

```
1. findById(id) → UsernameNotFoundException (404) si introuvable
2. Vérification unicité email/username (exclut l'utilisateur courant)
3. Mise à jour des champs (username, email, firstName, lastName, enabled, role)
4. Mise à jour mot de passe seulement si fourni (non vide)
5. userRepository.save() → UPDATE
```

### `AuthServiceImpl` — Orchestration

- `register()` → appelle UserService.register() + génère JWT
- `login()` → AuthenticationManager.authenticate() + génère JWT
- `logout()` → JwtTokenProvider.extractJti() + TokenBlacklistService.blacklist()

---

## 7. Couche Controller

### `AuthController` — Routes

```
POST /api/auth/register
  @Valid @RequestBody RegisterRequest
  → 201 Created + ApiResponse<AuthResponse>

POST /api/auth/login
  @Valid @RequestBody LoginRequest
  → 200 OK + ApiResponse<AuthResponse>

POST /api/auth/logout
  @RequestHeader Authorization: Bearer <token>
  → 200 OK + ApiResponse<Void>
```

### `UserController` — Routes (JWT requis pour toutes)

```
GET /api/users/me
  @AuthenticationPrincipal UserDetails
  → 200 OK + ApiResponse<AuthResponse.UserInfo>

GET /api/users
  → 200 OK + ApiResponse<List<UserDto>>

POST /api/users
  @Valid @RequestBody UserFormRequest
  → 201 Created + ApiResponse<UserDto>

GET /api/users/{id}
  @PathVariable Long id
  → 200 OK + ApiResponse<UserDto>

PUT /api/users/{id}
  @PathVariable Long id + @Valid @RequestBody UserFormRequest
  → 200 OK + ApiResponse<UserDto>

DELETE /api/users/{id}
  @PathVariable Long id
  → 200 OK + ApiResponse<Void>
```

---

## 8. Sécurité Spring Security + JWT

### Chaîne de filtres

```
Requête HTTP → CorsFilter → JwtAuthenticationFilter → SecurityFilterChain → Controller
```

### `SecurityConfig` — Règles d'autorisation

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/register", "/auth/login").permitAll()
    .anyRequest().authenticated()
)
.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
```

### Structure d'un JWT généré

```
Header  : { "alg": "HS256" }
Payload : {
  "sub": "jean@exemple.com",
  "jti": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "userId": 1,
  "username": "jean_dupont",
  "iat": 1705312200,
  "exp": 1705398600
}
Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
```

---

## 9. Redis — Blacklist JWT

### Format des clés Redis

```
Clé   : jwt:blacklist:<jti>
Valeur: "revoked"
TTL   : millisecondes restantes avant expiration du token
```

### Cycle de vie

```
Connexion  → token JWT émis (valide 24h)
Déconnexion → JTI stocké dans Redis avec TTL restant
Requête suivante → JwtAuthenticationFilter vérifie Redis → accès refusé
Expiration → Redis supprime automatiquement la clé → nettoyage automatique
```

---

## 10. Gestion des exceptions

### `GlobalExceptionHandler` — Table de correspondance

| Exception | HTTP | Message |
|-----------|------|---------|
| `MethodArgumentNotValidException` | 400 | Liste des champs invalides |
| `PasswordMismatchException` | 400 | "Les mots de passe ne correspondent pas" |
| `InvalidCredentialsException` | 401 | "Identifiants invalides" |
| `AccessDeniedException` | 403 | "Accès refusé" |
| `UsernameNotFoundException` | 404 | "Utilisateur non trouvé" |
| `UserAlreadyExistsException` | 409 | "Email/username déjà utilisé" |
| `Exception` (fallback) | 500 | "Erreur interne" |

---

## 11. Configuration

### Variables d'environnement

| Variable | Défaut | Description |
|----------|--------|-------------|
| `DB_HOST` | `localhost` | Hôte PostgreSQL |
| `DB_PORT` | `5432` | Port PostgreSQL |
| `DB_NAME` | `auth_db` | Nom de la base |
| `DB_USER` | `postgres` | Utilisateur DB |
| `DB_PASSWORD` | `postgres` | Mot de passe DB |
| `REDIS_HOST` | `localhost` | Hôte Redis |
| `REDIS_PORT` | `6379` | Port Redis |
| `JWT_SECRET` | *(défaut dev)* | Clé secrète HS256 (min 32 chars) |
| `JWT_EXPIRATION` | `86400000` | Expiration token (ms) |
| `SERVER_PORT` | `8080` | Port du serveur |

---

## 12. Tests API (exemples curl)

### Inscription

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jean_dupont",
    "email": "jean@exemple.com",
    "password": "MonMdp@123",
    "confirmPassword": "MonMdp@123",
    "firstName": "Jean",
    "lastName": "Dupont"
  }'
```

### Connexion

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "jean@exemple.com",
    "password": "MonMdp@123"
  }'
```

### Profil (JWT requis)

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGci..."
```

### Déconnexion

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer eyJhbGci..."
```
