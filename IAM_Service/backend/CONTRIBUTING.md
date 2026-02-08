# Guide de Contribution

Merci de votre intérêt pour contribuer au service IAM ! Ce guide vous aidera à démarrer.

## 📋 Table des matières

- [Code de conduite](#code-de-conduite)
- [Comment contribuer](#comment-contribuer)
- [Setup environnement de développement](#setup-environnement-de-développement)
- [Standards de code](#standards-de-code)
- [Processus de Pull Request](#processus-de-pull-request)
- [Conventions de commit](#conventions-de-commit)
- [Tests](#tests)

## Code de conduite

Soyez respectueux, inclusif et professionnel dans toutes vos interactions.

## Comment contribuer

### Signaler un bug

1. Vérifier que le bug n'est pas déjà signalé dans [Issues](../../issues)
2. Créer une nouvelle issue avec le template `bug_report`
3. Inclure :
   - Description claire du problème
   - Étapes pour reproduire
   - Comportement attendu vs actuel
   - Version du service
   - Logs pertinents

### Proposer une feature

1. Créer une issue avec le template `feature_request`
2. Décrire :
   - Le problème que ça résout
   - La solution proposée
   - Alternatives considérées

### Soumettre des modifications

1. Fork le repository
2. Créer une branche depuis `develop`
3. Faire vos modifications
4. Soumettre une Pull Request

## Setup environnement de développement

### Prérequis

```bash
# Vérifier les versions
java -version    # OpenJDK 21
mvn -version     # Maven 3.9+
docker --version # Docker 20.10+
```

### Installation

```bash
# 1. Cloner le repository
git clone https://github.com/votre-org/iam-service.git
cd iam-service

# 2. Démarrer l'infrastructure
cd IAM_SERVICE
docker compose up -d iam-db keycloak rabbitmq

# 3. Configurer l'IDE (IntelliJ IDEA recommandé)
# Importer le projet Maven
# Installer les plugins :
# - Lombok
# - SonarLint
# - CheckStyle

# 4. Lancer le service en mode dev
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Configuration de l'IDE

#### IntelliJ IDEA

```xml
<!-- Formatter : Google Java Style -->
1. Télécharger : https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml
2. Settings → Editor → Code Style → Java → Import Scheme
3. Sélectionner le fichier téléchargé
```

#### VS Code

```json
// .vscode/settings.json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml",
  "editor.formatOnSave": true
}
```

## Standards de code

### Architecture

**Règle d'or** : Respecter l'architecture hexagonale

```java
// ✅ BON : Domain ne dépend de rien
package com.example.iam.domain.model;

public class User {
    private UserId id;
    private Email email;
    // Pas d'annotations JPA, pas de dépendances Spring
}

// ❌ MAUVAIS : Domain dépend de l'infrastructure
package com.example.iam.domain.model;

import javax.persistence.Entity;

@Entity
public class User {
    // Violation de l'architecture hexagonale
}
```

### Organisation du code

```
src/main/java/com/example/iam/
├── domain/
│   ├── model/           # Entities, Value Objects
│   ├── repository/      # Repository interfaces (ports)
│   ├── service/         # Domain services
│   └── event/           # Domain events
├── application/
│   ├── usecase/         # Use cases (command handlers)
│   ├── query/           # Query handlers
│   └── port/            # Ports sortants (IdentityProviderPort)
├── infrastructure/
│   ├── persistence/     # JPA entities, repositories
│   ├── keycloak/        # Keycloak adapter
│   ├── messaging/       # RabbitMQ producer/consumer
│   └── config/          # Configuration Spring
└── interfaces/
    ├── rest/            # REST controllers
    └── event/           # Event listeners
```

### Conventions de nommage

| Type | Convention | Exemple |
|------|------------|---------|
| Class | PascalCase | `UserService` |
| Method | camelCase | `createUser()` |
| Variable | camelCase | `userId` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_ATTEMPTS` |
| Package | lowercase | `com.example.iam.domain` |
| Interface (Port) | Suffix "Port" | `IdentityProviderPort` |
| Use Case | Suffix "UseCase" | `CreateUserUseCase` |
| Value Object | Record si immuable | `record Email(String value)` |

### Style de code

#### Utiliser les Records Java pour les Value Objects

```java
// ✅ BON
public record Email(String value) {
    public Email {
        if (!isValid(value)) {
            throw new InvalidEmailException(value);
        }
    }
    
    private static boolean isValid(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}

// ❌ ÉVITER
public class Email {
    private final String value;
    
    public Email(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    // + equals, hashCode, toString...
}
```

#### Préférer l'immutabilité

```java
// ✅ BON : Immuable
public record CreateUserCommand(
    String email,
    String firstName,
    String lastName
) {}

// ❌ ÉVITER : Mutable
public class CreateUserCommand {
    private String email;
    
    public void setEmail(String email) {
        this.email = email;
    }
}
```

#### Gestion des erreurs

```java
// ✅ BON : Exceptions métier explicites
public class User {
    public void activate() {
        if (this.status == UserStatus.ACTIVE) {
            throw new UserAlreadyActiveException(this.id);
        }
        this.status = UserStatus.ACTIVE;
    }
}

// ❌ ÉVITER : Exceptions techniques génériques
public class User {
    public void activate() {
        if (this.status == UserStatus.ACTIVE) {
            throw new IllegalStateException("User already active");
        }
        this.status = UserStatus.ACTIVE;
    }
}
```

#### Éviter les null, préférer Optional

```java
// ✅ BON
public Optional<User> findByEmail(Email email) {
    return repository.findByEmail(email);
}

// Usage
userRepository.findByEmail(email)
    .orElseThrow(() -> new UserNotFoundException(email));

// ❌ ÉVITER
public User findByEmail(Email email) {
    return repository.findByEmail(email); // Peut retourner null
}
```

### Documentation

#### JavaDoc pour les APIs publiques

```java
/**
 * Crée un nouvel utilisateur dans le système.
 *
 * <p>Cette opération :
 * <ul>
 *   <li>Valide l'unicité de l'email</li>
 *   <li>Crée l'utilisateur dans la base de données</li>
 *   <li>Enregistre l'utilisateur dans Keycloak</li>
 *   <li>Publie un événement UserCreatedEvent</li>
 * </ul>
 *
 * @param command les données de création de l'utilisateur
 * @return l'utilisateur créé avec son ID généré
 * @throws UserAlreadyExistsException si l'email existe déjà
 * @throws IdentityProviderException si la création dans Keycloak échoue
 */
@Transactional
public User execute(CreateUserCommand command) {
    // Implementation
}
```

#### Commentaires pour la logique complexe

```java
// ✅ BON : Explique le pourquoi
// On doit synchroniser avec Keycloak avant de persister en DB
// pour éviter les incohérences si Keycloak échoue
String externalId = identityProvider.createUser(user);
user.linkToIdentityProvider(externalId);
userRepository.save(user);

// ❌ ÉVITER : Commentaire inutile qui décrit le quoi
// Crée l'utilisateur dans Keycloak
String externalId = identityProvider.createUser(user);
```

## Processus de Pull Request

### Checklist avant soumission

- [ ] Le code compile sans warning
- [ ] Les tests passent (`mvn verify`)
- [ ] La couverture de test n'a pas diminué
- [ ] Le code respecte le style Google Java
- [ ] Les tests ArchUnit passent (respect de l'architecture)
- [ ] La documentation est à jour si nécessaire
- [ ] Les logs sensibles sont supprimés
- [ ] Les secrets ne sont pas committés

### Template de Pull Request

```markdown
## Description
Brève description de ce que fait cette PR.

## Type de changement
- [ ] Bug fix (non-breaking change)
- [ ] Nouvelle feature (non-breaking change)
- [ ] Breaking change
- [ ] Documentation

## Motivation
Pourquoi ce changement est nécessaire ? Quel problème résout-il ?

Fixes #(numéro d'issue)

## Modifications
- Liste des changements principaux
- ...

## Tests
Comment les changements ont été testés ?

## Screenshots (si applicable)

## Checklist
- [ ] Mon code respecte le style du projet
- [ ] J'ai effectué une self-review
- [ ] J'ai commenté les parties complexes
- [ ] J'ai mis à jour la documentation
- [ ] Mes changements ne génèrent pas de warnings
- [ ] J'ai ajouté des tests qui prouvent que mon fix fonctionne
- [ ] Les tests nouveaux et existants passent
```

### Processus de review

1. **Auto-review** : Relire sa propre PR avant de la soumettre
2. **CI checks** : Vérifier que tous les checks passent (tests, linting, coverage)
3. **Review par les pairs** : Au moins 1 approbation requise
4. **Merge** : Squash and merge vers `develop`

## Conventions de commit

Nous utilisons [Conventional Commits](https://www.conventionalcommits.org/).

### Format

```
<type>(<scope>): <description>

[corps optionnel]

[footer optionnel]
```

### Types

- `feat`: Nouvelle fonctionnalité
- `fix`: Correction de bug
- `docs`: Documentation uniquement
- `style`: Formatage, point-virgules manquants, etc.
- `refactor`: Refactoring de code
- `perf`: Amélioration de performance
- `test`: Ajout de tests
- `chore`: Tâches de maintenance (build, CI, etc.)

### Scopes

- `user`: Gestion des utilisateurs
- `auth`: Authentification
- `keycloak`: Intégration Keycloak
- `api`: API REST
- `domain`: Couche domain
- `infra`: Infrastructure

### Exemples

```bash
# Feature
git commit -m "feat(user): add email verification endpoint"

# Bug fix
git commit -m "fix(auth): correct JWT issuer validation"

# Breaking change
git commit -m "feat(api)!: change user creation response format

BREAKING CHANGE: The response now returns userId instead of id"

# Documentation
git commit -m "docs(readme): update keycloak setup instructions"

# Refactoring
git commit -m "refactor(domain): extract email validation to value object"
```

## Tests

### Structure des tests

```
src/test/java/
├── unit/                    # Tests unitaires rapides (< 100ms)
│   └── domain/              # Tests du domain (pas de Spring)
├── integration/             # Tests d'intégration (Spring context)
│   ├── repository/          # Tests des repositories avec Testcontainers
│   ├── usecase/             # Tests des use cases
│   └── api/                 # Tests des endpoints REST
└── architecture/            # Tests ArchUnit
```

### Règles de test

1. **Couverture** : Minimum 80% pour les nouvelles features
2. **Nommage** : `methodName_condition_expectedResult`
3. **AAA Pattern** : Arrange, Act, Assert
4. **Isolation** : Chaque test doit être indépendant
5. **Fast** : Les tests unitaires doivent être rapides

### Exemples

#### Test unitaire du domain

```java
class EmailTest {
    
    @Test
    void of_withValidEmail_createsEmail() {
        // Arrange
        String validEmail = "user@example.com";
        
        // Act
        Email email = Email.of(validEmail);
        
        // Assert
        assertThat(email.value()).isEqualTo(validEmail);
    }
    
    @Test
    void of_withInvalidEmail_throwsException() {
        // Arrange
        String invalidEmail = "not-an-email";
        
        // Act & Assert
        assertThatThrownBy(() -> Email.of(invalidEmail))
            .isInstanceOf(InvalidEmailException.class)
            .hasMessageContaining(invalidEmail);
    }
}
```

#### Test d'intégration

```java
@SpringBootTest
@Testcontainers
class CreateUserUseCaseIT {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    
    @Autowired
    private CreateUserUseCase createUserUseCase;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @Transactional
    void execute_withValidCommand_createsUser() {
        // Arrange
        CreateUserCommand command = new CreateUserCommand(
            "test@example.com",
            "John",
            "Doe"
        );
        
        // Act
        User user = createUserUseCase.execute(command);
        
        // Assert
        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail().value()).isEqualTo("test@example.com");
        
        // Verify persistence
        Optional<User> savedUser = userRepository.findById(user.getId());
        assertThat(savedUser).isPresent();
    }
}
```

#### Test ArchUnit

```java
@AnalyzeClasses(packages = "com.example.iam")
class ArchitectureTest {
    
    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure = 
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");
    
    @ArchTest
    static final ArchRule use_cases_should_be_annotated = 
        classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .should().beAnnotatedWith(UseCase.class);
}
```

### Lancer les tests

```bash
# Tous les tests
mvn test

# Tests unitaires uniquement
mvn test -Dtest=**/*Test

# Tests d'intégration uniquement
mvn test -Dtest=**/*IT

# Avec rapport de couverture
mvn clean verify
open target/site/jacoco/index.html

# Tests d'un package spécifique
mvn test -Dtest=com.example.iam.domain.**
```

## Ressources utiles

### Documentation

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

### Outils

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)

### Contact

- **Questions** : Créer une [Discussion](../../discussions)
- **Bugs** : Créer une [Issue](../../issues)
- **Slack** : #iam-service (pour l'équipe interne)

---

Merci de contribuer au service IAM ! 🚀
