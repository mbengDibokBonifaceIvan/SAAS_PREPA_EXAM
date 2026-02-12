# Notification Service

## 📋 Vue d'ensemble

Service de notification asynchrone basé sur **Spring Boot 4.0.2** et conçu selon les principes de **Domain-Driven Design (DDD)**. Ce microservice gère l'envoi de notifications multi-canaux (Email, Push SSE, Toast) dans le cadre d'un SAAS qui sera utilisé pour les préparations des élèves aux concours.

### 🎯 Fonctionnalités principales

- ✉️ **Notifications Email** via SMTP (MailHog en développement)
- 🔔 **Notifications Push temps réel** via Server-Sent Events (SSE)
- 📊 **Historique paginé** des notifications par utilisateur
- 🔐 **Alertes de sécurité** (verrouillage, bannissement, réinitialisation)
- 👋 **Onboarding** (bienvenue organisation, activation compte, provisionnement)
- 📨 **Feedback générique** personnalisable
- 🐰 **Communication asynchrone** via RabbitMQ

---

## 🏗️ Architecture DDD

Le projet suit une architecture en couches respectant les principes du Domain-Driven Design :
```
notification-service/
├── application/          # Couche Application (Use Cases, DTOs, Ports)
│   ├── dto/             # Objets de transfert de données
│   ├── port/            
│   │   └── in/          # Ports entrants (interfaces des use cases)
│   │         
│   └── usecase/         # Implémentation des use cases
├── domain/              # Couche Domaine (Entités, Value Objects, Business Logic)
│   ├── entity/          # Entités
│   └── port
         └── out/        # Ports sortants (interfaces des adapters)
│   └── exception/       # Exceptions métier
├── infrastructure/      # Couche Infrastructure (Adapters, Config, Persistence)
│   ├── adapter/         
│   │   ├── in/          # Adapters entrants (RabbitMQ, REST)
│   │   └── out/         # Adapters sortants (Email, Database, Push)
│   ├── config/          # Configuration Spring
│   └── persistence/     # JPA Repositories
└── presentation/        # Couche Présentation (Controllers REST)
    └── v1/rest/         # API REST v1
```

### 🎨 Principes appliqués

- **Hexagonal Architecture** : Découplage du domaine via des ports et adapters
- **CQRS léger** : Séparation Commands/Queries dans les controllers
- **Inversion de dépendances** : Le domaine ne dépend d'aucune couche externe
- **Single Responsibility** : Chaque use case a une responsabilité unique

---

## 🚀 Démarrage rapide

### Prérequis

- **Java 21** (Eclipse Temurin recommandé)
- **Maven 3.9+**
- **Docker & Docker Compose** (pour l'environnement complet)

### Lancer avec Docker Compose
```bash
# Démarrer toute la stack (PostgreSQL, RabbitMQ, Keycloak, Consul, MailHog, Services)
docker compose up --build

# Vérifier les logs du service
docker logs -f notification-app

# Accéder à l'interface Swagger
open http://localhost:8082/swagger-ui.html

# Accéder à MailHog pour voir les mails 
open http://localhost:8025/
```

### Lancer en local (développement)
```bash
# 1. Démarrer uniquement les dépendances
docker compose up --build iam-db rabbitmq mailhog consul

# 2. Compiler et lancer le service
mvn clean install
mvn spring-boot:run

# Ou directement
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

---

## 📡 API REST

### Endpoints de commande (Commands)

**Base URL** : `http://localhost:8082/api/v1/notifications/commands`

#### 🔐 Alerte de sécurité
```bash
POST /security-alert
Content-Type: application/json

{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "name": "John Doe",
  "alertType": "ACCOUNT_LOCKED",
  "reason": "Tentatives de connexion échouées"
}
```

#### 🔑 Réinitialisation mot de passe
```bash
POST /password-reset
Content-Type: application/json

{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "name": "John Doe"
}
```

#### 👋 Bienvenue organisation
```bash
POST /welcome-org
Content-Type: application/json

{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "admin@company.com",
  "name": "Admin User",
  "detail": "Merci de rejoindre notre plateforme !"
}
```

#### ✅ Activation de compte
```bash
POST /account-activation
Content-Type: application/json

{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "name": "John Doe",
  "detail": "Votre compte est maintenant actif"
}
```

#### 📨 Feedback générique
```bash
POST /feedback
Content-Type: application/json

{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "subject": "Notification personnalisée",
  "message": "Contenu du message",
  "channel": "EMAIL"
}
```

#### 🔔 Abonnement Push (SSE)
```bash
GET /{userId}
Accept: text/event-stream

# Maintient une connexion ouverte pour recevoir les notifications en temps réel
```

### Endpoints de consultation (Queries)

**Base URL** : `http://localhost:8082/api/v1/notifications/queries`

#### 📜 Historique utilisateur (paginé)
```bash
GET /user/{userId}/history?page=0&size=10&sort=createdAt,desc

# Réponse :
{
  "content": [
    {
      "id": "uuid",
      "userId": "uuid",
      "type": "SECURITY_ALERT",
      "channel": "EMAIL",
      "status": "SENT",
      "createdAt": "2025-02-12T10:30:00Z"
    }
  ],
  "pageable": {...},
  "totalElements": 42,
  "totalPages": 5
}
```

---

## 🔧 Configuration

### Variables d'environnement

| Variable | Description | Défaut (local) | Docker |
|----------|-------------|----------------|--------|
| `SPRING_DATASOURCE_URL` | URL PostgreSQL | `jdbc:postgresql://localhost:5433/notification_db` | `jdbc:postgresql://iam-db:5432/notification_db` |
| `SPRING_DATASOURCE_USERNAME` | User DB | `user_admin` | `user_admin` |
| `SPRING_DATASOURCE_PASSWORD` | Password DB | `password_secure` | `password_secure` |
| `SPRING_RABBITMQ_HOST` | Host RabbitMQ | `localhost` | `rabbitmq` |
| `SPRING_MAIL_HOST` | Host SMTP | `localhost` | `mailhog` |
| `SPRING_MAIL_PORT` | Port SMTP | `1025` | `1025` |
| `SPRING_CLOUD_CONSUL_HOST` | Host Consul | `localhost` | `consul` |

### Profils Spring

- **default** : Configuration de base (local + Docker)
- **dev** : Mode développement avec logs verbeux
- **prod** : Configuration production (à définir selon l'infrastructure)

---

## 🧪 Tests & Qualité

### Couverture de code

- **Framework** : JaCoCo
- **Taux de couverture** : **91%** 📈
- **Nombre de tests** : **53 tests** ✅

### Lancer les tests
```bash
# Tests unitaires + intégration
mvn clean test

# Rapport JaCoCo
mvn jacoco:report
open target/site/jacoco/index.html

# Tests avec couverture (CI/CD)
mvn clean verify
```

### Structure des tests
```
src/test/java/
├── application/     # Tests des use cases
├── domain/          # Tests des entités et logique métier
├── infrastructure/  # Tests des adapters et repositories
└── presentation/    # Tests des controllers (MockMvc)
```

---

## 🐳 Docker

### Build de l'image
```bash
# Build manuel
docker build -t notification-service:latest .

# Via Docker Compose
docker compose build notification-service
```

### Optimisations Docker

- **Multi-stage build** : Séparation build Maven / runtime JRE
- **Layer caching** : Dépendances Maven mises en cache
- **JVM Java 21** : ZGC Generational pour des pauses GC minimales
- **Non-root user** : Exécution avec `springuser` pour la sécurité
- **Image minimale** : `eclipse-temurin:21-jre-jammy` (Ubuntu 22.04 LTS)

---

## 📊 Monitoring & Observabilité

### Consul Discovery

Le service s'enregistre automatiquement auprès de Consul :
- **Service Name** : `notification-service`
- **Instance ID** : `notification-service:${random.value}`
- **Console Consul** : http://localhost:8500

### Interfaces de monitoring

- **RabbitMQ Management** : http://localhost:15672 (guest/guest)
- **MailHog UI** : http://localhost:8025 (voir les emails interceptés)
- **Swagger UI** : http://localhost:8082/swagger-ui.html

---

## 🔄 Flux de données

### Publication RabbitMQ → Notification
```
┌─────────────┐      RabbitMQ       ┌──────────────────┐
│ Service     │ ───► user.events ───► │ RabbitListener   │
└─────────────┘      (Exchange)      │ (Infrastructure) │
                                      └────────┬─────────┘
                                               │
                                               ▼
                                      ┌──────────────────┐
                                      │ Use Case Handler │
                                      │ (Application)    │
                                      └────────┬─────────┘
                                               │
                                      ┌────────┴─────────┐
                                      │                  │
                                      ▼                  ▼
                              ┌─────────────┐    ┌─────────────┐
                              │ Email Sender│    │ Push Sender │
                              │ (SMTP)      │    │ (SSE)       │
                              └─────────────┘    └─────────────┘
```

---

## 🛠️ Technologies

| Catégorie | Technologie | Version |
|-----------|-------------|---------|
| **Framework** | Spring Boot | 4.0.2 |
| **Java** | Eclipse Temurin JDK | 21 |
| **Base de données** | PostgreSQL | 15 Alpine |
| **Messaging** | RabbitMQ | 3.12 Management |
| **Service Discovery** | Consul | Latest |
| **Mail (Dev)** | MailHog | Latest |
| **Tests** | JUnit 5, Mockito, Spring Test | - |
| **Couverture** | JaCoCo | - |
| **Documentation** | SpringDoc OpenAPI | 2.x |
| **Containerisation** | Docker | - |

---

## 📝 Modèle de données

### Entité Notification (Domain)
```java
@Entity
public class Notification {
    @Id private UUID id;
    private UUID userId;
    private String email;
    private NotificationType type;       // SECURITY_ALERT, ONBOARDING, FEEDBACK
    private NotificationChannel channel; // EMAIL, PUSH, TOAST
    private NotificationStatus status;   // PENDING, SENT, FAILED
    private String subject;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
```

---

## 🤝 Contribution

### Workflow Git
```bash
# 1. Créer une branche feature
git checkout -b feature/new-notification-type

# 2. Commits atomiques
git commit -m "feat(domain): add SMS notification channel"

# 3. Tests obligatoires (minimum 80% couverture)
mvn clean test jacoco:report

# 4. Push et Pull Request
git push origin feature/new-notification-type
```

### Convention de commits

- `feat`: Nouvelle fonctionnalité
- `fix`: Correction de bug
- `refactor`: Refactoring sans changement fonctionnel
- `test`: Ajout/modification de tests
- `docs`: Documentation uniquement

---

## 📄 Licence

Ce projet est un microservice interne du système IAM. Tous droits réservés.

---

## 👥 Auteur

**Ivan** - Développeur Backend

---

## 🔗 Liens utiles

- [Documentation Spring Boot 3](https://docs.spring.io/spring-boot/docs/3.4.x/reference/html/)
- [Domain-Driven Design - Eric Evans](https://www.domainlanguage.com/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)# Notification Service
