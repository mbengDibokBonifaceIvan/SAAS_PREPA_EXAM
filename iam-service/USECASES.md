# Use Cases - IAM Service

Ce document détaille les cas d'usage métier du service IAM dans le contexte d'une plateforme de gestion de centres d'examens.

## 📋 Table des matières

- [Acteurs](#acteurs)
- [Use Cases d'Authentification](#use-cases-dauthentification)
- [Use Cases de Gestion de Compte](#use-cases-de-gestion-de-compte)
- [Use Cases de Consultation](#use-cases-de-consultation)
- [Règles Métier](#règles-métier)

## Acteurs

### 👤 Anonyme
Visiteur non authentifié de la plateforme.

### 🏢 CENTER_OWNER (Propriétaire de centre)
- **Créé lors de** : Onboarding de l'organisation
- **Droits** : Accès complet à l'organisation
- **Limites** : Un seul par organisation

### 👥 UNIT_MANAGER (Gestionnaire d'unité)
- **Créé par** : CENTER_OWNER
- **Droits** : Gestion de son unité et des utilisateurs de l'unité
- **Limites** : Accès limité à son unité

### 🧑‍💼 STAFF_MEMBER (Membre du personnel)
- **Créé par** : CENTER_OWNER, UNIT_MANAGER
- **Droits** : Conduite d'examens, provisionnement de candidats
- **Limites** : Peut uniquement créer des candidats

### 🎓 CANDIDATE (Candidat)
- **Créé par** : STAFF_MEMBER, UNIT_MANAGER, CENTER_OWNER
- **Droits** : Passage d'examens, consultation de résultats
- **Limites** : Accès lecture seule à son profil

---

## Use Cases d'Authentification

### UC-AUTH-01 : Onboarding d'une organisation

**Acteur** : Anonyme

**Description** : Un nouveau client souhaite enregistrer son organisation sur la plateforme.

**Pré-conditions** :
- L'utilisateur n'est pas authentifié
- L'email et le nom d'organisation ne sont pas déjà utilisés

**Flow principal** :
1. L'utilisateur accède à la page d'inscription
2. Il saisit :
   - Nom de l'organisation
   - Ses informations personnelles (prénom, nom, email)
   - Un mot de passe
3. Le système :
   - Crée l'organisation
   - Crée le compte CENTER_OWNER
   - Enregistre l'utilisateur dans Keycloak
   - Envoie un email de bienvenue
4. L'utilisateur reçoit un email de confirmation

**Post-conditions** :
- Une nouvelle organisation est créée avec statut ACTIVE
- Le CENTER_OWNER est créé et activé
- Un événement `OrganizationCreatedEvent` est publié

**Exceptions** :
- Email déjà utilisé → 409 Conflict
- Nom d'organisation déjà utilisé → 409 Conflict
- Mot de passe faible → 400 Bad Request

**Endpoint** : `POST /v1/auth/onboarding`

**Exemple de requête** :
```json
{
  "ownerFirstName": "Marie",
  "ownerLastName": "Dupont",
  "ownerEmail": "contact@examparis.fr",
  "ownerPassword": "SecurePass123!",
  "organizationName": "Centre d'Examens de Paris"

}
```

---

### UC-AUTH-02 : Connexion

**Acteur** : Utilisateur enregistré (tous rôles)

**Description** : Un utilisateur se connecte à la plateforme.

**Pré-conditions** :
- L'utilisateur a un compte actif
- Le compte n'est pas banni

**Flow principal** :
1. L'utilisateur saisit son email et mot de passe
2. Le système valide les credentials avec Keycloak
3. Le système retourne un access token et refresh token

**Post-conditions** :
- L'utilisateur est authentifié
- Un access token JWT est généré
- La date de dernière connexion est mise à jour

**Exceptions** :
- Credentials invalides → 401 Unauthorized
- Compte banni → 403 Forbidden
- Compte non vérifié → 403 Forbidden

**Endpoint** : `POST /v1/auth/login`

---

### UC-AUTH-03 : Déconnexion

**Acteur** : Utilisateur authentifié

**Description** : Un utilisateur se déconnecte de la plateforme.

**Flow principal** :
1. L'utilisateur clique sur déconnexion
2. Le système révoque le refresh token dans Keycloak

**Post-conditions** :
- Le refresh token est invalidé
- L'access token reste valide jusqu'à expiration (nature stateless du JWT)

**Endpoint** : `POST /v1/auth/logout`

---

### UC-AUTH-04 : Mot de passe oublié

**Acteur** : Utilisateur enregistré ou Anonyme

**Description** : Un utilisateur demande la réinitialisation de son mot de passe.

**Flow principal** :
1. L'utilisateur saisit son email
2. Le système :
   - Vérifie si l'email existe (sans le révéler)
   - Génère un lien de réinitialisation via Keycloak
   - Envoie un email avec le lien
3. L'utilisateur reçoit l'email et peut réinitialiser

**Post-conditions** :
- Un email de réinitialisation est envoyé (si le compte existe)
- Un événement `PasswordResetRequestedEvent` est publié

**Note de sécurité** : Le système ne révèle jamais si l'email existe ou non (protection contre l'énumération).

**Endpoint** : `POST /v1/auth/forgot-password`

---

## Use Cases de Gestion de Compte

### UC-ACCOUNT-01 : Provisionner un compte Staff

**Acteur** : CENTER_OWNER ou UNIT_MANAGER

**Description** : Un administrateur crée un compte pour un membre du personnel.

**Pré-conditions** :
- L'utilisateur est authentifié
- L'utilisateur a le rôle CENTER_OWNER ou UNIT_MANAGER
- L'email du nouveau compte n'existe pas

**Flow principal** :
1. L'administrateur saisit :
   - Email, prénom, nom du nouveau membre
   - Rôle (UNIT_MANAGER ou STAFF_MEMBER)
   - Unité d'affectation (si applicable)
2. Le système :
   - Valide les permissions (voir règles métier)
   - Crée le compte en DB
   - Crée le compte dans Keycloak
   - Envoie un email d'activation
3. Le nouveau membre reçoit un email pour activer son compte

**Post-conditions** :
- Un nouveau compte est créé avec statut `active=false`
- Un événement `UserProvisionedEvent` est publié
- Un email d'activation est envoyé

**Règles métier** :
- CENTER_OWNER peut créer : UNIT_MANAGER, STAFF_MEMBER, CANDIDATE (dans son centre uniquement)
- UNIT_MANAGER peut créer : STAFF_MEMBER, CANDIDATE (dans son unité uniquement)
- STAFF_MEMBER peut créer : CANDIDATE (dans son unité uniquement)

**Endpoint** : `POST /v1/accounts/provision`

**Exemple** :
```json
{
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "staff@example.com",
    "role": "STAFF_MEMBER",
    "unitId": "550e8400-e29b-41d4-a716-446655440001"
}
```

---

### UC-ACCOUNT-02 : Provisionner un candidat

**Acteur** : STAFF_MEMBER, UNIT_MANAGER ou CENTER_OWNER

**Description** : Un membre du personnel enregistre un nouveau candidat.

**Pré-conditions** :
- L'utilisateur est authentifié
- L'email du candidat n'existe pas

**Flow principal** :
1. Le personnel saisit les informations du candidat
2. Le système crée le compte avec rôle CANDIDATE
3. Un email d'activation est envoyé au candidat

**Post-conditions** :
- Un compte CANDIDATE est créé
- Un événement `UserProvisionedEvent` est publié

**Particularité** : 
- STAFF_MEMBER peut UNIQUEMENT créer des CANDIDATE (pas d'autres rôles)

**Endpoint** : `POST /v1/accounts/provision`

---

### UC-ACCOUNT-03 : Bannir un compte

**Acteur** : CENTER_OWNER uniquement

**Description** : Le propriétaire désactive le compte d'un utilisateur.

**Pré-conditions** :
- L'utilisateur est CENTER_OWNER
- Le compte cible n'est pas un CENTER_OWNER

**Flow principal** :
1. Le CENTER_OWNER sélectionne un utilisateur
2. Il clique sur "Bannir"
3. Le système :
   - Désactive le compte (active=false)
   - Désactive le compte dans Keycloak
   - Publie un événement

**Post-conditions** :
- Le compte est désactivé
- L'utilisateur ne peut plus se connecter
- Un événement `UserBannedEvent` est publié

**Règles métier** :
- Un CENTER_OWNER ne peut pas se bannir lui-même
- Un CENTER_OWNER ne peut pas bannir un autre CENTER_OWNER

**Endpoint** : `PATCH /v1/accounts/{email}/ban`

---

### UC-ACCOUNT-04 : Activer un compte

**Acteur** : CENTER_OWNER uniquement

**Description** : Le propriétaire réactive un compte précédemment banni.

**Flow principal** :
1. Le CENTER_OWNER sélectionne un utilisateur banni
2. Il clique sur "Activer"
3. Le système réactive le compte

**Post-conditions** :
- Le compte est réactivé (enabled=true)
- Un événement `UserActivatedEvent` est publié

**Endpoint** : `PATCH /v1/accounts/{email}/activate`

---

### UC-ACCOUNT-05 : Mettre à jour un profil

**Acteur** : CENTER_OWNER, UNIT_MANAGER, STAFF_MEMBER (propre profil)

**Description** : Un utilisateur met à jour ses informations ou celles d'un autre utilisateur.

**Pré-conditions** :
- L'utilisateur est authentifié
- L'utilisateur a les permissions nécessaires

**Flow principal** :
1. L'utilisateur modifie :
   - Prénom, nom
   - Unité d'affectation (si autorisé)
2. Le système valide et sauvegarde

**Post-conditions** :
- Le profil est mis à jour
- Un événement `UserUpdatedEvent` est publié

**Règles métier** :
- STAFF_MEMBER peut uniquement modifier son propre profil (firstName, lastName)
- UNIT_MANAGER peut modifier les utilisateurs de son unité
- CENTER_OWNER peut modifier tous les utilisateurs

**Endpoint** : `PUT /v1/accounts/{id}`

---

## Use Cases de Consultation

### UC-CONSULT-01 : Consulter mon profil

**Acteur** : Utilisateur authentifié (tous rôles)

**Description** : Un utilisateur consulte ses propres informations.

**Flow principal** :
1. L'utilisateur accède à son profil
2. Le système extrait l'email du JWT
3. Le système retourne les informations du profil

**Post-conditions** : Aucune

**Endpoint** : `GET /v1/accounts/me`

**Réponse** :
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

### UC-CONSULT-02 : Consulter l'annuaire (CENTER_OWNER)

**Acteur** : CENTER_OWNER

**Description** : Le propriétaire consulte tous les utilisateurs de son organisation.

**Pré-conditions** :
- L'utilisateur est CENTER_OWNER

**Flow principal** :
1. Le CENTER_OWNER accède à l'annuaire
2. Le système retourne tous les utilisateurs de l'organisation
3. Il peut filtrer par unité (optionnel)

**Post-conditions** : Aucune

**Endpoint** : `GET /v1/accounts/directory` ou `GET /v1/accounts/directory?unitId={uuid}`

**Réponse** : Liste de tous les utilisateurs de l'organisation

---

### UC-CONSULT-03 : Consulter l'annuaire (UNIT_MANAGER)

**Acteur** : UNIT_MANAGER

**Description** : Le gestionnaire consulte les utilisateurs de son unité.

**Pré-conditions** :
- L'utilisateur est UNIT_MANAGER

**Flow principal** :
1. Le UNIT_MANAGER accède à l'annuaire
2. Le système retourne uniquement les utilisateurs de son unité

**Post-conditions** : Aucune

**Règle métier** :
- Un UNIT_MANAGER ne peut voir que les utilisateurs de son unité
- Il ne peut pas voir les utilisateurs des autres unités
- Le OWNER_CENTER peut avoir accès à cette route si le sous centre appartient à son centre.

**Endpoint** : `GET /v1/accounts/directory`

---

### UC-CONSULT-04 : Consulter un utilisateur par ID

**Acteur** : CENTER_OWNER, UNIT_MANAGER ou STAFF_MEMBER

**Description** : Récupération des détails d'un utilisateur spécifique.

**Pré-conditions** :
- L'utilisateur demandeur a les permissions
- L'utilisateur cible existe

**Flow principal** :
1. L'utilisateur fournit l'ID de l'utilisateur cible
2. Le système vérifie les permissions
3. Le système retourne les détails

**Règles métier** :
- CENTER_OWNER peut consulter tous les utilisateurs de l'organisation
- UNIT_MANAGER peut consulter uniquement les utilisateurs de son unité
- STAFF_MEMBER peut consulter uniquement les candidats de son unité

**Endpoint** : `GET /v1/accounts/{id}`

---

## Règles Métier

### RG-01 : Hiérarchie des rôles

```
CENTER_OWNER (Propriétaire)
    │
    ├── UNIT_MANAGER (Gestionnaire d'unité)
    │       │
    │       ├── STAFF_MEMBER (Personnel)
    │       │       │
    │       │       └── CANDIDATE (Candidat)
    │       │
    │       └── CANDIDATE
    │
    └── STAFF_MEMBER
            │
            └── CANDIDATE
```

### RG-02 : Matrice de permissions (Provisionnement)

| Créateur ↓ / Cible → | CENTER_OWNER | UNIT_MANAGER | STAFF_MEMBER | CANDIDATE |
|----------------------|--------------|--------------|--------------|-----------|
| **CENTER_OWNER**     | ❌           | ✅           | ✅           | ✅        |
| **UNIT_MANAGER**     | ❌           | ❌           | ✅           | ✅        |
| **STAFF_MEMBER**     | ❌           | ❌           | ❌           | ✅        |
| **CANDIDATE**        | ❌           | ❌           | ❌           | ❌        |

### RG-03 : Matrice de permissions (Consultation)

| Rôle | Peut consulter |
|------|----------------|
| **CENTER_OWNER** | Tous les utilisateurs de l'organisation |
| **UNIT_MANAGER** | Utilisateurs de son unité uniquement |
| **STAFF_MEMBER** | Candidats de son unité uniquement |
| **CANDIDATE** | Son propre profil uniquement |

### RG-04 : Matrice de permissions (Modification)

| Rôle | Peut modifier |
|------|---------------|
| **CENTER_OWNER** | Tous les utilisateurs (sauf autres CENTER_OWNER) |
| **UNIT_MANAGER** | Utilisateurs de son unité (sauf CENTER_OWNER) |
| **STAFF_MEMBER** | Son propre profil (firstName, lastName uniquement) |
| **CANDIDATE** | Son propre profil (firstName, lastName uniquement) |

### RG-05 : Règles de bannissement

- Seul CENTER_OWNER peut bannir/activer des comptes
- Un CENTER_OWNER ne peut pas se bannir lui-même
- Un CENTER_OWNER ne peut pas bannir un autre CENTER_OWNER
- Un compte banni ne peut plus se connecter
- Toutes les sessions actives sont révoquées lors du bannissement

### RG-06 : Règles de scope d'unité

- Un UNIT_MANAGER ne peut créer des utilisateurs QUE dans son unité
- Un STAFF_MEMBER ne peut créer des candidats QUE dans son unité
- Un utilisateur ne peut être affecté qu'à UNE SEULE unité
- CENTER_OWNER n'a pas d'unité (accès global)

### RG-07 : Règles de validation email

- Format email valide requis
- Unicité de l'email dans toute la plateforme (pas seulement l'organisation)
- Email non modifiable après création
- Email obligatoire pour tous les utilisateurs

### RG-08 : Règles de mot de passe

- Minimum 8 caractères
- Au moins 1 majuscule
- Au moins 1 minuscule
- Au moins 1 chiffre
- Au moins 1 caractère spécial
- Ne peut pas contenir l'email

### RG-09 : Multi-tenancy

- Chaque organisation est isolée (tenant)
- Un utilisateur appartient à UNE SEULE organisation
- Les données sont filtrées par organization_id
- Aucun partage de données entre organisations

### RG-10 : Événements métier

Tous les use cases majeurs publient un événement :

| Use Case | Événement publié |
|----------|------------------|
| Onboarding | `OrganizationCreatedEvent` + `OwnerCreatedEvent` |
| Provision User | `UserProvisionedEvent` |
| Ban Account | `UserBannedEvent` |
| Activate Account | `UserActivatedEvent` |
| Update Profile | `UserUpdatedEvent` |
| Forgot Password | `PasswordResetRequestedEvent` |

Ces événements permettent aux autres services de réagir (ex: envoi d'emails, audit, analytics).

---

## Diagrammes de flux

### Flux d'onboarding

```
┌────────────┐
│   Anonyme  │
└─────┬──────┘
      │
      ▼
┌─────────────────────────────────┐
│ Saisie informations organisation│
│ + informations propriétaire     │
└─────┬───────────────────────────┘
      │
      ▼
┌─────────────────────────────────┐
│  Validation & création          │
│  - Organization                 │
│  - CENTER_OWNER                 │
│  - Compte Keycloak              │
└─────┬───────────────────────────┘
      │
      ▼
┌─────────────────────────────────┐
│  Email de bienvenue envoyé      │
└─────┬───────────────────────────┘
      │
      ▼
┌─────────────────────────────────┐
│  CENTER_OWNER peut se connecter │
│  et commencer à provisionner    │
└─────────────────────────────────┘
```

### Flux de provisionnement

```
┌──────────────────┐
│ CENTER_OWNER ou  │
│ UNIT_MANAGER ou  │
│ STAFF_MEMBER     │
└────────┬─────────┘
         │
         ▼
┌─────────────────────────────┐
│ Vérification permissions    │
│ (selon matrice RG-02)       │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ Création compte             │
│ - DB                        │
│ - Keycloak                  │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ Email d'activation envoyé   │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ Nouvel utilisateur reçoit   │
│ email et active son compte  │
└─────────────────────────────┘
```

---

**Dernière mise à jour** : Février 2026  
**Version** : 1.0.0
