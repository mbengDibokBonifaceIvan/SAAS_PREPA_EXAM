# API Documentation - IAM Service

Base URL: `http://localhost:8081/v1`

## Authentification

### Login

Authentification avec email et mot de passe.

**Endpoint:** `POST /auth/login`

**Permissions:** Public

**Request Body:**

```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response:** `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 300
}
```

**Error Responses:**

- `401 Unauthorized` - Identifiants invalides
- `400 Bad Request` - Données de requête invalides

**cURL Example:**

```bash
curl -X POST http://localhost:8081/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }'
```

---

### Logout

Déconnexion et révocation du refresh token.

**Endpoint:** `POST /auth/logout`

**Permissions:** Authenticated

**Request Body:**

```json
{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:** `204 No Content`

**cURL Example:**

```bash
curl -X POST http://localhost:8081/v1/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

---

### Utiliser le token

```bash
curl -X GET http://localhost:8081/v1/accounts/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Endpoints

### Authentication

#### Onboarding (Enregistrement d'organisation)

Enregistre une nouvelle organisation avec son propriétaire (CENTER_OWNER).

**Endpoint:** `POST /auth/onboarding`

**Permissions:** Public

**Request Body:**

```json
{
  "ownerFirstName": "John",
  "ownerLastName": "Doe",
  "ownerEmail": "owner@example.com",
  "ownerPassword": "SecurePassword123!",
  "organizationName": "Mon Centre d'Examens",

}
```

**Response:** `201 Created`

```json
{

  "ownerFirstName": "John",
  "ownerLastName": "Doe",
  "isActive": "false",
  "externalOrganizationId": "550e8400-e29b-41d4-a716-446655440000",
  "mustChangePassword": "false",
  "isEmailVerified": "false"
}
```

**Error Responses:**

- `400 Bad Request` - Données invalides
- `409 Conflict` - Organisation ou email déjà existant

**cURL Example:**

```bash
curl -X POST http://localhost:8081/v1/auth/onboarding \
  -H "Content-Type: application/json" \
  -d '{
    "ownerFirstName": "John",
    "ownerLastName": "Doe",
    "ownerEmail": "owner@example.com",
    "ownerPassword": "SecurePassword123!",
    "organizationName": "Mon Centre d'Examens",
}'
```

---

#### Forgot Password

Demande de réinitialisation du mot de passe.

**Endpoint:** `POST /auth/forgot-password`

**Permissions:** Public

**Request Body:**

```json
{
  "email": "user@example.com"
}
```

**Response:** `200 OK`

```json
{
  "message": "Si un compte est associé à cet email, une procédure de réinitialisation a été envoyée."
}
```

**cURL Example:**

```bash
curl -X POST http://localhost:8081/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

---

### Account Management

### Account Management

#### Provision Account

Provisionne un nouveau compte (Staff ou Candidat). Accessible uniquement aux CENTER_OWNER, UNIT_MANAGER ou STAFF_MEMBER.

**Endpoint:** `POST /accounts/provision`

**Permissions:** `CENTER_OWNER`, `UNIT_MANAGER`, `STAFF_MEMBER`

**Request Body:**

```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "staff@example.com",
  "role": "STAFF_MEMBER",
  "unitId": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Response:** `201 Created`

```json
{
  "message": "Utilisateur provisionné avec succès. Un email d'activation lui a été envoyé."
}
```

**Error Responses:**

- `400 Bad Request` - Données invalides
- `403 Forbidden` - Permissions insuffisantes
- `409 Conflict` - Email déjà utilisé

**cURL Example:**

```bash
curl -X POST http://localhost:8081/v1/accounts/provision \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "staff@example.com",
    "role": "STAFF_MEMBER",
    "unitId": "550e8400-e29b-41d4-a716-446655440001"
  }'
```

---

#### Get My Profile

Récupère le profil de l'utilisateur authentifié.

**Endpoint:** `GET /accounts/me`

**Permissions:** Authenticated user

**Response:** `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "firstName": "John",
  "lastName": "Doe",
  "email": "user@example.com",
  "role": "CENTER_OWNER",
  "tenantId": "550e8400-e29b-41d4-a716-446655440005",
  "unitId": "550e8400-e29b-41d4-a716-446655440002",
  "active": true
}
```

**cURL Example:**

```bash
curl -X GET http://localhost:8081/v1/accounts/me \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

#### Get Directory

Récupère l'annuaire des utilisateurs selon les droits de l'utilisateur authentifié.

**Endpoint:** `GET /accounts/directory`

**Permissions:** `CENTER_OWNER`, `UNIT_MANAGER`

**Query Parameters:**

- `unitId` (UUID, optional) - Filtrer par unité

**Response:** `200 OK`

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "John",
    "lastName": "Doe",
    "email": "user1@example.com",
    "role": "STAFF_MEMBER",
    "tenantId": "550e8400-e29b-41d4-a716-446655440001",
    "unitId": "550e8400-e29b-41d4-a716-446655440002",
    "active": true
  },

  {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "user2@example.com",
    "role": "UNIT_MANAGER",
    "tenantId": "550e8400-e29b-41d4-a716-446655440008",
    "unitId": "550e8400-e29b-41d4-a716-446655440001",
    "active": true,
  }
]
```

**cURL Examples:**

```bash
# Tous les utilisateurs (CENTER_OWNER)
curl -X GET http://localhost:8081/v1/accounts/directory \
  -H "Authorization: Bearer YOUR_TOKEN"

# Filtre par unité
curl -X GET "http://localhost:8081/v1/accounts/directory?unitId=550e8400-e29b-41d4-a716-446655440001" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

#### Get User by ID

Récupère les informations d'un utilisateur par son ID.

**Endpoint:** `GET /accounts/{id}`

**Permissions:** `CENTER_OWNER`, `UNIT_MANAGER`

**Path Parameters:**

- `id` (UUID) - ID de l'utilisateur

**Response:** `200 OK`

```json
  {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "user2@example.com",
    "role": "UNIT_MANAGER",
    "tenantId": "550e8400-e29b-41d4-a716-446655440008",
    "unitId": "550e8400-e29b-41d4-a716-446655440001",
    "active": true,
  }
```

**Error Responses:**

- `404 Not Found` - Utilisateur non trouvé
- `403 Forbidden` - Permissions insuffisantes

**cURL Example:**

```bash
curl -X GET http://localhost:8081/v1/accounts/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

#### Update User

Met à jour les informations d'un utilisateur.

**Endpoint:** `PUT /accounts/{id}`

**Permissions:** `CENTER_OWNER`, `UNIT_MANAGER`, `STAFF_MEMBER` (avec restrictions)

**Path Parameters:**

- `id` (UUID) - ID de l'utilisateur

**Request Body:**

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "role": "UNIT_MANAGER",
  "unitId": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Response:** `204 No Content`

**Error Responses:**

- `404 Not Found` - Utilisateur non trouvé
- `403 Forbidden` - Permissions insuffisantes
- `400 Bad Request` - Données invalides

**cURL Example:**

```bash
curl -X PUT http://localhost:8081/v1/accounts/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "role": "UNIT_MANAGER",
    "unitId": "550e8400-e29b-41d4-a716-446655440001"
  }'
```

---

#### Ban Account

Désactive le compte d'un utilisateur.

**Endpoint:** `PATCH /accounts/{email}/ban`

**Permissions:** `CENTER_OWNER` uniquement

**Path Parameters:**

- `email` (string) - Email de l'utilisateur à bannir

**Response:** `204 No Content`

**Error Responses:**

- `404 Not Found` - Utilisateur non trouvé
- `403 Forbidden` - Seul le CENTER_OWNER peut bannir
- `409 Conflict` - L'utilisateur est déjà banni

**cURL Example:**

```bash
curl -X PATCH http://localhost:8081/v1/accounts/user@example.com/ban \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

#### Activate Account

Active le compte d'un utilisateur précédemment banni.

**Endpoint:** `PATCH /accounts/{email}/activate`

**Permissions:** `CENTER_OWNER` uniquement

**Path Parameters:**

- `email` (string) - Email de l'utilisateur à activer

**Response:** `204 No Content`

**Error Responses:**

- `404 Not Found` - Utilisateur non trouvé
- `403 Forbidden` - Seul le CENTER_OWNER peut activer
- `409 Conflict` - L'utilisateur est déjà actif

**cURL Example:**

```bash
curl -X PATCH http://localhost:8081/v1/accounts/user@example.com/activate \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### Roles & Permissions

Le système utilise les rôles suivants avec une hiérarchie de permissions :

| Rôle             | Description            | Permissions                                         |
| ---------------- | ---------------------- | --------------------------------------------------- |
| **CENTER_OWNER** | Propriétaire du centre | Tous les accès, gestion de l'organisation           |
| **UNIT_MANAGER** | Gestionnaire d'unité   | Gestion de son unité et des utilisateurs de l'unité |
| **STAFF_MEMBER** | Membre du personnel    | Accès limité selon les fonctionnalités              |
| **CANDIDATE**    | Candidat               | Accès aux examens et résultats personnels           |

### Modèles de données

#### UserResponse

```json
  {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "user2@example.com",
    "role": "UNIT_MANAGER",
    "tenantId": "550e8400-e29b-41d4-a716-446655440008",
    "unitId": "550e8400-e29b-41d4-a716-446655440001",
    "active": true,
  }
```
---

## 🛠 Gestion des Erreurs (API Standard RFC 7807)

L'API utilise le format Problem Details for HTTP APIs pour fournir des erreurs descriptives et actionnables. Toutes les réponses d'erreur incluent un type pointant vers la documentation et un timestamp.

### 400 Bad Request

Retourné en cas d'erreur de syntaxe JSON ou de violation de règle métier simple.

```json
{
  "type": "https://api.exams.com/errors/business-rule-violation",
  "title": "Règle métier violée",
  "status": 400,
  "detail": "Le nom de l'organisation ne peut pas être vide",
  "instance": "/api/v1/users",
  "timestamp": "2026-02-08T11:45:00.000Z"
}
```

### Erreur de Validation (DTO)

Lorsqu'un ou plusieurs champs ne respectent pas les contraintes @Valid, l'objet contient un champ invalid_params.

```json
{
  "type": "https://api.exams.com/errors/validation-error",
  "title": "Validation échouée",
  "status": 400,
  "detail": "Champs invalides",
  "instance": "/api/v1/users",
  "timestamp": "2026-02-08T11:45:10.000Z",
  "invalid_params": {
    "email": "doit être une adresse email bien formée",
    "password": "doit contenir au moins 8 caractères"
  }
}
```

### 401 Unauthorized

Retourné lorsque le jeton (Token) est manquant, expiré ou invalide.

```json
{
  "type": "https://api.exams.com/errors/identity-service-error",
  "title": "Erreur Service Identité",
  "status": 401,
  "detail": "Identifiants invalides ou session expirée",
  "instance": "/api/v1/resource",
  "timestamp": "2026-02-08T11:46:00.000Z"
}
```

### 403 Forbidden

Retourné en cas de compte verrouillé ou de permissions insuffisantes pour accéder à une ressource spécifique.

```json
{
  "type": "https://api.exams.com/errors/insufficient-privileges",
  "title": "Droits insuffisants",
  "status": 403,
  "detail": "Vous n'avez pas le rôle OWNER_CENTER requis pour cette action",
  "instance": "/api/v1/admin/settings",
  "timestamp": "2026-02-08T11:47:00.000Z"
}
```

### 404 Not Found

Retourné lorsqu'une ressource (utilisateur, organisation, etc.) n'existe pas en base.

```json
{
  "type": "https://api.exams.com/errors/not-found",
  "title": "Ressource non trouvée",
  "status": 404,
  "detail": "Unable to find com.ivan.backend.User with id 550e8400...",
  "instance": "/api/v1/users/550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-02-08T11:48:00.000Z"
}
```

### 409 Conflict

Retourné lorsqu'un utilisateur avec le même email existe déjà ou qu'une contrainte de base de données est violée.

```json
{
  "type": "https://api.exams.com/errors/user-already-exists",
  "title": "Conflit d'identité",
  "status": 409,
  "detail": "Un utilisateur existe déjà avec l'email: test@example.com",
  "instance": "/api/v1/users",
  "timestamp": "2026-02-08T11:49:00.000Z"
}
```

### 500 Internal Server Error

Retourné en cas d'erreur imprévue ou de panne du service d'identité (Keycloak).

```json
{
  "type": "https://api.exams.com/errors/internal-server-error",
  "title": "Erreur Interne",
  "status": 500,
  "detail": "Une erreur inattendue est survenue.",
  "instance": "/api/v1/users",
  "timestamp": "2026-02-08T11:50:00.000Z"
}
```
---

## Webhooks / Events

Le service IAM publie des événements sur RabbitMQ pour notifier les autres services.

### Événements disponibles

| Événement              | Exchange              | Routing Key                     | Description                                 |
| ---------------------- | --------------------- | ------------------------------- | ------------------------------------------- |
| OrganizationCreated    | `iam.exchange` | `organization.registered`          | Organisation créée lors de l'onboarding     |
| UserProvisioned        | `iam.exchange`         | `user.provisioned`              | Utilisateur provisionné (Staff/Candidat)    |
| UserActivated          | `iam.exchange`         | `account.activated`                | Compte utilisateur activé                   |
| UserBanned             | `iam.exchange`         | `account.banned`                   | Compte utilisateur banni                    |
| PasswordResetRequested | `iam.exchange`         | `password.reset.requested` | Demande de réinitialisation de mot de passe |
| UserUpdated            | `iam.exchange`         | `user.updated`                  | Profil utilisateur mis à jour               |

---

## OpenAPI / Swagger

Documentation interactive disponible à : http://localhost:8081/swagger-ui.html

Spécification OpenAPI : http://localhost:8081/v3/api-docs
