# IAM Service (Identity & Access Management)

[![Test Coverage](https://img.shields.io/badge/coverage-67%25-yellow.svg)](./docs/coverage)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Keycloak](https://img.shields.io/badge/Keycloak-26.0.0-blue.svg)](https://www.keycloak.org/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Microservice de gestion centralisée de l'identité et des accès pour la plateforme SaaS de gestion de centres d'examens. Ce service fournit l'authentification multi-tenant, l'autorisation basée sur les rôles (RBAC) et la gestion complète du cycle de vie des utilisateurs (CENTER_OWNER, UNIT_MANAGER, STAFF_MEMBER, CANDIDATE).

## 📋 Table des matières

- [Architecture](#-architecture)
- [Prérequis](#-prérequis)
- [Installation rapide](#-installation-rapide)
- [Configuration](#%EF%B8%8F-configuration)
- [Utilisation](#-utilisation)
- [API Documentation](#-api-documentation)
- [Tests](#-tests)
- [Déploiement](#-déploiement)
- [Troubleshooting](#-troubleshooting)
- [Contribution](#-contribution)

## 🏗 Architecture

### Stack technologique

- **Backend**: Spring Boot 4.0.2 (Java 21)
- **Architecture**: Domain-Driven Design (DDD) + Architecture Hexagonale
- **Identity Provider**: Keycloak 26.0.0
- **Base de données**: PostgreSQL 15
- **Message Broker**: RabbitMQ 3.12
- **Conteneurisation**: Docker & Docker Compose

### Principes architecturaux

```
src/
├── domain/              # Logique métier pure (entities, value objects, event, exceptions,domain services)
├── application/         # Use cases, dto, mapper
├── infrastructure/      # Implémentations techniques (repositories, adapters, config)
│   ├── persistence/
│   ├── messaging/
│   └── keycloak/
└── presentation/          # Points d'entrée (REST controllers)
```

**Hexagonal Architecture (Ports & Adapters)**:
- **Domain Layer**: Aucune dépendance externe
- **Application Layer**: Orchestration des cas d'usage
- **Infrastructure Layer**: Implémentation des adaptateurs (Keycloak, PostgreSQL, RabbitMQ)
- **Presentation Layer**: APIs REST et consumers d'événements

## 🔧 Prérequis

### Obligatoires
- Docker Engine >= 27.5.1
- Docker Compose >= 2.37.3
- Git

### Pour le développement local (optionnel)
- Java 21 (Eclipse Temurin recommandé)
- Maven 3.9+
- Un IDE (IntelliJ IDEA, VS Code avec extensions Java)

## 🚀 Installation rapide

### Démarrage avec Docker Compose (recommandé)

```bash
# 1. Cloner le repository
git clone https://github.com/mbengDibokBonifaceIvan/SAAS_PREPA_EXAM.git
cd IAM_SERVICE

# 2. Lancer tous les services
docker compose up --build

# 3. Vérifier que tous les services sont opérationnels
docker compose ps
```

**Services démarrés** :
- **IAM Service** : http://localhost:8081
- **Keycloak Admin Console** : http://localhost:8080 (admin/admin_password)
- **RabbitMQ Management** : http://localhost:15672 (guest/guest)
- **PostgreSQL** : localhost:5433

### Configuration automatique

Le fichier `backend/realm-export.json` est **automatiquement importé** au démarrage de Keycloak. Il contient :
- ✅ Realm `ExamsRealm` pré-configuré
- ✅ Client `iam-admin-client` avec ses credentials
- ✅ Rôles `CENTER_OWNER`, `UNIT_MANAGER`, `STAFF_MEMBER` et `CANDIDAT`
- ✅ Service Account avec permissions `manage-users`, `view-users`, `query-users`, `manage-realm`

> ⚠️ **Important** : Le mot de passe SMTP Gmail doit être configuré manuellement (voir [Configuration SMTP](#smtp-gmail)).

## ⚙️ Configuration

### Variables d'environnement

Les variables sont définies dans `docker-compose.yml`. Pour les modifier en développement :

```yaml
environment:
  # Database
  - SPRING_DATASOURCE_URL=jdbc:postgresql://iam-db:5432/iam_logic_db
  - SPRING_DATASOURCE_USERNAME=user_admin
  - SPRING_DATASOURCE_PASSWORD=password_secure
  
  # RabbitMQ
  - SPRING_RABBITMQ_HOST=rabbitmq
  - SPRING_RABBITMQ_USERNAME=guest
  - SPRING_RABBITMQ_PASSWORD=guest
  
  # Keycloak Client
  - KEYCLOAK_CLIENT_SECRET=k2jrL3KQ0lNNY3VF8sZxLQ3azw0FUi36
  - KEYCLOAK_SERVER_URL=http://iam-keycloak:8080
  
  # JWT Validation
  - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8080/realms/ExamsRealm
  - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://iam-keycloak:8080/realms/ExamsRealm/protocol/openid-connect/certs
```

### SMTP (Gmail)

Pour activer l'envoi d'emails (réinitialisation de mot de passe, vérification) :

#### Méthode 1 : Via l'interface Keycloak (recommandé pour le développement)

1. Accéder à http://localhost:8080
2. Se connecter (admin/admin_password)
3. Sélectionner le realm **ExamsRealm**
4. Aller dans **Realm Settings** → Onglet **Email**
5. Configurer :
   ```
   From: votre-email@gmail.com
   Host: smtp.gmail.com
   Port: 587
   Authentication: ON
   Username: votre-email@gmail.com
   Password: [Mot de passe d'application 16 caractères]
   Connection Security: Enable StartTLS
   ```
6. Générer un mot de passe d'application Google : https://myaccount.google.com/apppasswords
7. Cliquer sur **Test connection**

#### Méthode 2 : Modifier le fichier realm-export.json (pour automatisation)

Éditer `backend/realm-export.json` et localiser la section `smtpServer` :

```json
"smtpServer": {
  "from": "votre-email@gmail.com",
  "host": "smtp.gmail.com",
  "port": "587",
  "auth": "true",
  "user": "votre-email@gmail.com",
  "password": "VOTRE_MOT_DE_PASSE_DAPPLICATION",
  "starttls": "true"
}
```

## 📖 Utilisation

### Configuration manuelle de Keycloak (alternative à l'import automatique)

Si vous préférez configurer Keycloak manuellement :

<details>
<summary><b>Étape 1 : Créer le Realm</b></summary>

1. Cliquer sur le menu déroulant (affichant "Master")
2. **Create Realm**
3. Realm name : `ExamsRealm`
4. **Create**
</details>

<details>
<summary><b>Étape 2 : Créer le Client Admin</b></summary>

1. **Clients** → **Create client**
2. Client ID : `iam-admin-client`
3. **Capability Config** :
   - ✅ Client Authentication: ON
   - ✅ Service Accounts Roles: ON
   - ✅ Standard Flow: ON
   - ✅ Direct Access Grants: ON
4. **Login Settings** :
   - Valid Redirect URIs : `*`
   - Web Origins : `*`
5. **Save**
6. Onglet **Credentials** → Copier le **Client Secret** → Mettre à jour dans `docker-compose.yml`
</details>

<details>
<summary><b>Étape 3 : Assigner les rôles au Service Account</b></summary>

1. Client `iam-admin-client` → Onglet **Service accounts roles**
2. **Assign role**
3. Filter by clients
4. Chercher `realm-management`
5. Sélectionner :
   - ✅ `manage-users`
   - ✅ `view-users`
   - ✅ `query-users`
   - ✅ `manage-realm`
6. **Assign**
</details>

### Exporter la configuration Keycloak

Pour sauvegarder votre configuration :

1. **Realm Settings** → **Action** → **Partial Export**
2. Cocher :
   - ✅ Export groups and roles
   - ✅ Export clients
3. **Export**
4. Renommer le fichier en `realm-export.json`
5. Placer à `backend/realm-export.json`

> ⚠️ **Attention** : Le mot de passe SMTP n'est pas exporté par sécurité. L'ajouter manuellement dans le JSON.


## 📚 API Documentation

### Endpoints principaux

#### Authentication
| Méthode | Endpoint | Description | Auth requise |
|---------|----------|-------------|--------------|
| POST | `/v1/auth/onboarding` | Enregistrement d'un chef de centre et de son organisation | ❌ Public |
| POST | `/v1/auth/login` | Authentification | ❌ Public |
| POST | `/v1/auth/logout` | Déconnexion | ✅ User |
| POST | `/v1/auth/forgot-password` | Réinitialisation de mot de passe | ❌ Public |

#### Account Management
| Méthode | Endpoint | Description | Auth requise |
|---------|----------|-------------|--------------|
| POST | `/v1/accounts/provision` | Provisionner un compte Staff/Candidat | ✅ CENTER_OWNER/UNIT_MANAGER/STAFF |
| GET | `/v1/accounts/me` | Récupérer mon profil | ✅ User |
| GET | `/v1/accounts/directory` | Récupérer l'annuaire | ✅ CENTER_OWNER/UNIT_MANAGER |
| GET | `/v1/accounts/{id}` | Récupérer un utilisateur | ✅ CENTER_OWNER/UNIT_MANAGER |
| PUT | `/v1/accounts/{id}` | Mettre à jour un utilisateur | ✅ CENTER_OWNER/UNIT_MANAGER/STAFF |
| PATCH | `/v1/accounts/{email}/ban` | Bannir un compte | ✅ CENTER_OWNER |
| PATCH | `/v1/accounts/{email}/activate` | Activer un compte | ✅ CENTER_OWNER |

### Rôles et permissions

| Rôle | Description | Cas d'usage |
|------|-------------|-------------|
| **CENTER_OWNER** | Propriétaire du centre | Créé lors de l'onboarding, accès complet à l'organisation |
| **UNIT_MANAGER** | Gestionnaire d'unité | Gestion des utilisateurs et examens de son unité |
| **STAFF_MEMBER** | Membre du personnel | Conduite d'examens, gestion de candidats |
| **CANDIDATE** | Candidat | Passage d'examens, consultation de résultats |

### Authentification

Toutes les requêtes nécessitent un JWT Bearer token obtenu via l'endpoint `/v1/auth/login` :

```bash
# 1. Login
curl -X POST http://localhost:8081/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@example.com",
    "password": "SecurePassword123!"
  }'

# 2. Utiliser le token
curl -X GET http://localhost:8081/v1/accounts/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

Pour plus de détails, consultez [API.md](./API.md).

## 🧪 Tests

### Couverture actuelle : 84% avec plus de 100 tests exécutés.

```bash
# Exécuter tous les tests
mvn test

# Avec rapport de couverture (à voir grâce au fichier target/site/index.html qui sera généré)
mvn clean verify

```

### Structure des tests

```
src/test/java/com/ivan/backend/
├── domain/              
├── application/       
├── infrastructure/
└── presentation/  
```

**Objectif** : Atteindre 100% de couverture dans les prochaines itérations.

## 🚢 Déploiement

### Environnement de production

**Changements recommandés** :

```yaml
# docker-compose.prod.yml
services:
  iam-db:
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD_FROM_SECRETS}
    volumes:
      - /data/postgres:/var/lib/postgresql/data  # Persistance

  keycloak:
    command: start --optimized  # Mode production
    environment:
      KC_HOSTNAME: iam.votredomaine.com
      KC_PROXY: edge  # Si derrière un reverse proxy
      KC_DB_PASSWORD: ${DB_PASSWORD_FROM_SECRETS}
      KC_BOOTSTRAP_ADMIN_PASSWORD: ${ADMIN_PASSWORD_FROM_SECRETS}

  iam-service:
    environment:
      KEYCLOAK_CLIENT_SECRET: ${CLIENT_SECRET_FROM_SECRETS}
      SPRING_PROFILES_ACTIVE: prod
```

### Health checks

```bash
# IAM Service
curl http://localhost:8081/actuator/health

# Keycloak
curl http://localhost:8080/health/ready
```

## 🔍 Troubleshooting

### Problème : Keycloak ne démarre pas

**Solution** : Vérifier les logs et augmenter le `start_period` du healthcheck

```bash
docker compose logs keycloak
```

### Problème : Erreur JWT "Invalid issuer"

**Cause** : Différence entre l'issuer du token et celui configuré

**Solution** : Vérifier la cohérence des URLs dans `docker-compose.yml` :
```yaml
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8080/realms/ExamsRealm
```

### Problème : Tests échouent en local

**Solution** : Utiliser Testcontainers ou un profil de test

```bash
mvn test -Dspring.profiles.active=test
```

### Logs et debugging

```bash
# Tous les logs
docker compose logs -f

# Service spécifique
docker compose logs -f iam-service

# Logs Keycloak
docker compose logs -f keycloak
```

## 🤝 Contribution

### Workflow Git

1. Créer une branche depuis `develop`
   ```bash
   git checkout -b feature/nom-de-la-feature
   ```
2. Faire vos modifications
3. Lancer les tests : `mvn verify`
4. Commit avec messages conventionnels :
   ```bash
   git commit -m "feat(user): add email verification endpoint"
   ```
5. Créer une Pull Request vers `develop`

### Conventions de code

- **Style** : Google Java Style Guide
- **Architecture** : Respect strict des couches (vérifiable via ArchUnit tests)
- **Tests** : Minimum 80% de couverture pour les nouvelles features
- **Messages de commit** : [Conventional Commits](https://www.conventionalcommits.org/)

### Standards de qualité

- ✅ Pas de dépendances du domain vers l'infrastructure
- ✅ Tous les use cases ont des tests
- ✅ Les controllers ne contiennent pas de logique métier
- ✅ Gestion des erreurs avec des exceptions métier

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 📞 Support

- **Documentation API** : http://localhost:8081/swagger-ui.html 
- **Issues** : [GitHub Issues](https://github.com/votre-org/iam-service/issues)
- **Wiki** : [Documentation complète](https://github.com/votre-org/iam-service/wiki)

---

**Dernière mise à jour** : Février 2026  
**Version** : 1.0.0  
**Mainteneurs** : Équipe Infrastructure
