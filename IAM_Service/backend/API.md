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
  "organizationName": "Mon Centre d'Examens",
  "ownerEmail": "owner@example.com",
  "ownerFirstName": "John",
  "ownerLastName": "Doe",
  "ownerPassword": "SecurePassword123!"
}
```

**Response:** `201 Created`
```json
{
  "organizationId": "550e8400-e29b-41d4-a716-446655440000",
  "organizationName": "Mon Centre d'Examens",
  "ownerEmail": "owner@example.com",
  "message": "Organisation créée avec succès"
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
    "organizationName": "Mon Centre d'\''Examens",
    "ownerEmail": "owner@example.com",
    "ownerFirstName": "John",
    "ownerLastName": "Doe",
    "ownerPassword": "SecurePassword123!"
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
  "email": "staff@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
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
    "email": "staff@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
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
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "CENTER_OWNER",
  "unitId": null,
  "organizationId": "550e8400-e29b-41d4-a716-446655440002",
  "enabled": true,
  "emailVerified": true,
  "createdAt": "2026-02-08T10:30:00Z",
  "updatedAt": "2026-02-08T10:35:00Z"
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
    "email": "user1@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "STAFF_MEMBER",
    "unitId": "550e8400-e29b-41d4-a716-446655440001",
    "enabled": true,
    "createdAt": "2026-02-08T10:30:00Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "email": "user2@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "role": "UNIT_MANAGER",
    "unitId": "550e8400-e29b-41d4-a716-446655440001",
    "enabled": true,
    "createdAt": "2026-02-08T11:15:00Z"
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
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "STAFF_MEMBER",
  "unitId": "550e8400-e29b-41d4-a716-446655440001",
  "organizationId": "550e8400-e29b-41d4-a716-446655440002",
  "enabled": true,
  "emailVerified": true,
  "createdAt": "2026-02-08T10:30:00Z",
  "updatedAt": "2026-02-08T10:35:00Z"
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

| Rôle | Description | Permissions |
|------|-------------|-------------|
| **CENTER_OWNER** | Propriétaire du centre | Tous les accès, gestion de l'organisation |
| **UNIT_MANAGER** | Gestionnaire d'unité | Gestion de son unité et des utilisateurs de l'unité |
| **STAFF_MEMBER** | Membre du personnel | Accès limité selon les fonctionnalités |
| **CANDIDATE** | Candidat | Accès aux examens et résultats personnels |

### Modèles de données

#### UserResponse

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "STAFF_MEMBER",
  "unitId": "550e8400-e29b-41d4-a716-446655440001",
  "organizationId": "550e8400-e29b-41d4-a716-446655440002",
  "enabled": true,
  "emailVerified": true,
  "createdAt": "2026-02-08T10:30:00Z",
  "updatedAt": "2026-02-08T10:35:00Z"
}
```

---

#### Health Check

Vérifie l'état du service.

**Endpoint:** `GET /actuator/health`

**Permissions:** Public

**Response:** `200 OK`
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "keycloak": {
      "status": "UP"
    },
    "rabbitmq": {
      "status": "UP"
    }
  }
}
```

---

#### Service Info

Informations sur le service.

**Endpoint:** `GET /actuator/info`

**Permissions:** Public

**Response:** `200 OK`
```json
{
  "app": {
    "name": "IAM Service",
    "version": "1.0.0",
    "description": "Identity and Access Management Service"
  },
  "build": {
    "artifact": "iam-service",
    "name": "iam-service",
    "time": "2026-02-08T10:00:00Z",
    "version": "1.0.0"
  }
}
```

---

## Error Responses

Tous les endpoints peuvent retourner les erreurs suivantes :

### 400 Bad Request
```json
{
  "timestamp": "2026-02-08T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address",
      "rejectedValue": "invalid-email"
    }
  ],
  "path": "/api/v1/users"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2026-02-08T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/users"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2026-02-08T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. Required role: ADMIN",
  "path": "/api/v1/users"
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-02-08T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 550e8400-e29b-41d4-a716-446655440000",
  "path": "/api/v1/users/550e8400-e29b-41d4-a716-446655440000"
}
```

### 409 Conflict
```json
{
  "timestamp": "2026-02-08T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Resource already exists",
  "path": "/api/v1/users"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2026-02-08T10:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/v1/users"
}
```

---

## Rate Limiting

| Plan | Requests per minute | Burst |
|------|---------------------|-------|
| Free | 60 | 10 |
| Pro | 600 | 100 |

Dépassement de limite :
```json
{
  "timestamp": "2026-02-08T10:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 30 seconds.",
  "retryAfter": 30
}
```

---

## Webhooks / Events

Le service IAM publie des événements sur RabbitMQ pour notifier les autres services.

### Événements disponibles

| Événement | Exchange | Routing Key | Description |
|-----------|----------|-------------|-------------|
| OrganizationCreated | `organization.events` | `organization.created` | Organisation créée lors de l'onboarding |
| UserProvisioned | `user.events` | `user.provisioned` | Utilisateur provisionné (Staff/Candidat) |
| UserActivated | `user.events` | `user.activated` | Compte utilisateur activé |
| UserBanned | `user.events` | `user.banned` | Compte utilisateur banni |
| UserUpdated | `user.events` | `user.updated` | Profil utilisateur mis à jour |
| PasswordResetRequested | `user.events` | `user.password.reset_requested` | Demande de réinitialisation de mot de passe |

### Format des événements

```json
{
  "eventId": "uuid",
  "eventType": "UserProvisionedEvent",
  "aggregateId": "user-uuid",
  "occurredAt": "2026-02-08T10:30:00Z",
  "version": 1,
  "payload": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "STAFF_MEMBER",
    "organizationId": "550e8400-e29b-41d4-a716-446655440002",
    "unitId": "550e8400-e29b-41d4-a716-446655440001",
    "createdBy": "owner@example.com"
  }
}
```

---

### Health & Monitoring

## Postman Collection

Une collection Postman est disponible : [Download Collection](./postman/IAM-Service.postman_collection.json)

Import dans Postman :
1. File → Import
2. Sélectionner le fichier JSON
3. Configurer l'environnement avec votre token

---

## OpenAPI / Swagger

Documentation interactive disponible à : http://localhost:8081/swagger-ui.html

Spécification OpenAPI : http://localhost:8081/v3/api-docs
