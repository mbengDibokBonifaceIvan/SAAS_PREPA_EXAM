# Architecture Documentation - IAM Service

## Vue d'ensemble

Ce document détaille les choix architecturaux du service IAM et les patterns utilisés.

## Architecture Hexagonale (Ports & Adapters)

### Principes fondamentaux

L'architecture hexagonale permet de séparer la logique métier des détails techniques, rendant le code :
- **Testable** : Le domain peut être testé sans dépendances externes
- **Maintenable** : Les changements d'infrastructure n'impactent pas le métier
- **Flexible** : Remplacement facile des adaptateurs (ex: changer Keycloak pour Auth0)

### Structure des couches

```
┌─────────────────────────────────────────────────────────────┐
│                    Interfaces Layer                          │
│  (REST Controllers, Event Listeners, Scheduled Tasks)       │
└────────────────┬────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────┐
│                  Application Layer                           │
│         (Use Cases, Command/Query Handlers)                  │
└────────────────┬────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────┐
│                    Domain Layer                              │
│  (Entities, Value Objects, Domain Services, Repositories)   │
│                   ⚠️ AUCUNE DÉPENDANCE                       │
└────────────────┬────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────┐
│                Infrastructure Layer                          │
│   (JPA Repositories, Keycloak Adapter, RabbitMQ Producer)   │
└─────────────────────────────────────────────────────────────┘
```

### Exemple concret : Provisionnement d'un utilisateur

#### 1. **Interface Layer** - Point d'entrée
```java
@RestController
@RequestMapping("/v1/accounts")
public class AccountManagementController {
    
    private final ProvisionUserInputPort provisionUserUseCase;
    
    @PostMapping("/provision")
    @PreAuthorize("hasAnyRole('CENTER_OWNER', 'UNIT_MANAGER', 'STAFF_MEMBER')")
    public ResponseEntity<Map<String, String>> provisionAccount(
            @RequestBody ProvisionUserRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String creatorEmail = jwt.getClaimAsString("email");
        provisionUserUseCase.execute(request, creatorEmail);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            Map.of("message", "Utilisateur provisionné avec succès. Un email d'activation lui a été envoyé.")
        );
    }
}
```

#### 2. **Application Layer** - Orchestration du use case
```java
@UseCase
@Transactional
public class ProvisionUserUseCase implements ProvisionUserInputPort {
    
    private final UserRepository userRepository;  // Port
    private final OrganizationRepository organizationRepository;  // Port
    private final IdentityProviderPort identityProvider;  // Port
    private final UserProvisionedEventPublisher eventPublisher;  // Port
    
    @Override
    public void execute(ProvisionUserRequest request, String creatorEmail) {
        // 1. Récupérer le créateur
        User creator = userRepository.findByEmail(Email.of(creatorEmail))
            .orElseThrow(() -> new UserNotFoundException(creatorEmail));
        
        // 2. Validation des permissions
        validateCreatorPermissions(creator, request.role());
        
        // 3. Vérifier que l'email n'existe pas
        if (userRepository.findByEmail(Email.of(request.email())).isPresent()) {
            throw new UserAlreadyExistsException(request.email());
        }
        
        // 4. Récupérer l'organisation et l'unité
        Organization organization = creator.getOrganization();
        Unit unit = request.unitId() != null 
            ? organization.getUnit(UnitId.of(request.unitId()))
            : null;
        
        // 5. Créer l'utilisateur (domain logic)
        User user = User.provision(
            Email.of(request.email()),
            UserProfile.of(request.firstName(), request.lastName()),
            UserRole.of(request.role()),
            organization,
            unit,
            creator
        );
        
        // 6. Sauvegarder en DB
        userRepository.save(user);
        
        // 7. Créer dans Keycloak
        String externalId = identityProvider.provisionUser(user, request.sendActivationEmail());
        user.linkToIdentityProvider(externalId);
        
        // 8. Publier l'événement
        eventPublisher.publish(UserProvisionedEvent.from(user, creatorEmail));
    }
    
    private void validateCreatorPermissions(User creator, String targetRole) {
        if (creator.hasRole(UserRole.STAFF_MEMBER) && 
            !targetRole.equals("CANDIDATE")) {
            throw new InsufficientPermissionsException(
                "STAFF_MEMBER can only provision CANDIDATE"
            );
        }
        
        if (creator.hasRole(UserRole.UNIT_MANAGER) && 
            targetRole.equals("CENTER_OWNER")) {
            throw new InsufficientPermissionsException(
                "UNIT_MANAGER cannot provision CENTER_OWNER"
            );
        }
    }
}
```

#### 3. **Domain Layer** - Logique métier pure
```java
@Entity
public class User {
    
    @Id
    private UserId id;
    
    @Embedded
    private Email email;
    
    @Embedded
    private UserProfile profile;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    private String externalIdentityId;
    
    // Factory method
    public static User create(UserCommand command) {
        return new User(
            UserId.generate(),
            Email.of(command.email()),
            UserProfile.of(command.firstName(), command.lastName()),
            UserStatus.PENDING_VERIFICATION
        );
    }
    
    // Domain logic
    public void linkToIdentityProvider(String externalId) {
        if (this.externalIdentityId != null) {
            throw new UserAlreadyLinkedException();
        }
        this.externalIdentityId = externalId;
    }
    
    public void activate() {
        if (this.status == UserStatus.ACTIVE) {
            throw new UserAlreadyActiveException();
        }
        this.status = UserStatus.ACTIVE;
        // Déclenche un domain event
        registerDomainEvent(new UserActivatedDomainEvent(this.id));
    }
}
```

#### 4. **Infrastructure Layer** - Implémentation technique

**Repository JPA**
```java
@Repository
class JpaUserRepository implements UserRepository {
    
    private final SpringDataUserRepository springRepo;
    
    @Override
    public void save(User user) {
        UserEntity entity = UserEntityMapper.toEntity(user);
        springRepo.save(entity);
    }
    
    @Override
    public Optional<User> findByEmail(Email email) {
        return springRepo.findByEmail(email.value())
            .map(UserEntityMapper::toDomain);
    }
}
```

**Keycloak Adapter**
```java
@Component
class KeycloakIdentityProviderAdapter implements IdentityProviderPort {
    
    private final Keycloak keycloak;
    
    @Override
    public String createUser(User user) {
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setEmail(user.getEmail().value());
        kcUser.setFirstName(user.getProfile().firstName());
        kcUser.setLastName(user.getProfile().lastName());
        kcUser.setEnabled(true);
        
        Response response = keycloak.realm("ExamsRealm")
            .users()
            .create(kcUser);
            
        return extractUserId(response);
    }
}
```

## Domain-Driven Design (DDD)

### Bounded Context

Le service IAM constitue un **Bounded Context** dans l'architecture microservices de la plateforme de gestion de centres d'examens :

- **Responsabilité principale** : Gestion de l'identité et de l'authentification multi-tenant
- **Entités métier** : Organization, User, Unit
- **Rôles gérés** : CENTER_OWNER, UNIT_MANAGER, STAFF_MEMBER, CANDIDATE
- **Événements publiés** : OrganizationCreated, UserProvisioned, UserActivated, UserBanned
- **Événements consommés** : (selon les besoins des autres services)

**Relations avec d'autres Bounded Contexts** :
- **Exam Management Context** : Consomme les événements UserProvisioned pour associer les candidats aux examens
- **Notification Context** : Consomme les événements UserProvisioned, PasswordResetRequested pour l'envoi d'emails
- **Audit Context** : Consomme tous les événements IAM pour la traçabilité

### Building Blocks utilisés

#### Value Objects
```java
public record Email(String value) {
    
    public Email {
        if (!isValid(value)) {
            throw new InvalidEmailException(value);
        }
    }
    
    private static boolean isValid(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
    
    public static Email of(String value) {
        return new Email(value);
    }
}

public record UserRole(String value) {
    
    public UserRole {
        if (!isValid(value)) {
            throw new InvalidRoleException(value);
        }
    }
    
    private static boolean isValid(String role) {
        return Set.of("CENTER_OWNER", "UNIT_MANAGER", "STAFF_MEMBER", "CANDIDATE")
            .contains(role);
    }
}
```

#### Aggregates

**Organization Aggregate**
```java
@Entity
public class Organization /* extends AggregateRoot */ {
    
    @Id
    private OrganizationId id;  // Identity
    
    @Embedded
    private OrganizationName name;
    
    @OneToOne
    private User owner;  // CENTER_OWNER
    
    @OneToMany(cascade = CascadeType.ALL)
    private Set<Unit> units;  // Aggregate children
    
    private OrganizationStatus status;
    
    // Factory method
    public static Organization create(OrganizationName name, User owner) {
        Organization org = new Organization(
            OrganizationId.generate(),
            name,
            owner,
            OrganizationStatus.ACTIVE
        );
        org.registerDomainEvent(new OrganizationCreatedEvent(org));
        return org;
    }
    
    // Domain logic
    public void addUnit(Unit unit) {
        if (this.status != OrganizationStatus.ACTIVE) {
            throw new OrganizationNotActiveException();
        }
        this.units.add(unit);
        registerDomainEvent(new UnitAddedEvent(this.id, unit.getId()));
    }
}
```

**User Aggregate**
```java
@Entity
public class User /* extends AggregateRoot */ {
    
    @Id
    private UserId id;
    
    @Embedded
    private Email email;
    
    @Embedded
    private UserProfile profile;  // firstName, lastName
    
    @Enumerated(EnumType.STRING)
    private UserRole role;  // CENTER_OWNER, UNIT_MANAGER, STAFF_MEMBER, CANDIDATE
    
    @ManyToOne
    private Organization organization;
    
    @ManyToOne
    private Unit unit;  // Optionnel pour CENTER_OWNER
    
    private boolean enabled;
    private boolean emailVerified;
    private String externalIdentityId;  // Keycloak ID
    
    // Factory method for Onboarding
    public static User createOwner(
        Email email, 
        UserProfile profile, 
        Organization organization
    ) {
        User owner = new User(
            UserId.generate(),
            email,
            profile,
            UserRole.CENTER_OWNER,
            organization,
            null,  // Pas d'unité pour le owner
            true,
            false
        );
        owner.registerDomainEvent(new OwnerCreatedEvent(owner));
        return owner;
    }
    
    // Factory method for Provisioning
    public static User provision(
        Email email,
        UserProfile profile,
        UserRole role,
        Organization organization,
        Unit unit,
        User creator
    ) {
        User user = new User(
            UserId.generate(),
            email,
            profile,
            role,
            organization,
            unit,
            false,  // Désactivé jusqu'à activation email
            false
        );
        user.registerDomainEvent(
            new UserProvisionedEvent(user, creator.getEmail())
        );
        return user;
    }
    
    // Domain logic
    public void ban(User bannedBy) {
        if (!bannedBy.hasRole(UserRole.CENTER_OWNER)) {
            throw new InsufficientPermissionsException();
        }
        if (this.role == UserRole.CENTER_OWNER) {
            throw new CannotBanOwnerException();
        }
        this.enabled = false;
        registerDomainEvent(new UserBannedEvent(this.id, bannedBy.getId()));
    }
    
    public void activate(User activatedBy) {
        if (!activatedBy.hasRole(UserRole.CENTER_OWNER)) {
            throw new InsufficientPermissionsException();
        }
        this.enabled = true;
        registerDomainEvent(new UserActivatedEvent(this.id, activatedBy.getId()));
    }
    
    public boolean canAccessUnit(Unit unit) {
        if (this.role == UserRole.CENTER_OWNER) {
            return unit.belongsTo(this.organization);
        }
        return this.unit != null && this.unit.equals(unit);
    }
}
```

#### Domain Events
```java
public record OrganizationCreatedEvent(
    UUID organizationId,
    String organizationName,
    UUID ownerId,
    String ownerEmail,
    Instant occurredAt
) implements DomainEvent {
    
    public static OrganizationCreatedEvent from(Organization org) {
        return new OrganizationCreatedEvent(
            org.getId().value(),
            org.getName().value(),
            org.getOwner().getId().value(),
            org.getOwner().getEmail().value(),
            Instant.now()
        );
    }
}

public record UserProvisionedEvent(
    UUID userId,
    String email,
    String role,
    UUID organizationId,
    UUID unitId,
    String createdByEmail,
    Instant occurredAt
) implements DomainEvent {
    
    public static UserProvisionedEvent from(User user, String creatorEmail) {
        return new UserProvisionedEvent(
            user.getId().value(),
            user.getEmail().value(),
            user.getRole().value(),
            user.getOrganization().getId().value(),
            user.getUnit() != null ? user.getUnit().getId().value() : null,
            creatorEmail,
            Instant.now()
        );
    }
}
```

#### Repositories (Port)
```java
public interface UserRepository {
    void save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(Email email);
    List<User> findByOrganization(OrganizationId organizationId);
    List<User> findByUnit(UnitId unitId);
    void delete(UserId id);
}

public interface OrganizationRepository {
    void save(Organization organization);
    Optional<Organization> findById(OrganizationId id);
    Optional<Organization> findByName(OrganizationName name);
}
```

### Ubiquitous Language

| Terme Métier | Définition | Implémentation |
|--------------|------------|----------------|
| **Organization** | Centre d'examens (multi-tenant) | `Organization` aggregate |
| **Unit** | Unité/département au sein d'un centre | `Unit` entity |
| **CENTER_OWNER** | Propriétaire du centre, créé lors de l'onboarding | `UserRole.CENTER_OWNER` |
| **UNIT_MANAGER** | Responsable d'une unité | `UserRole.UNIT_MANAGER` |
| **STAFF_MEMBER** | Membre du personnel | `UserRole.STAFF_MEMBER` |
| **CANDIDATE** | Candidat aux examens | `UserRole.CANDIDATE` |
| **Provisioning** | Création d'un compte par un admin | `ProvisionUserUseCase` |
| **Onboarding** | Enregistrement initial d'une organisation | `OnboardingUseCase` |
| **Ban** | Désactivation d'un compte par le owner | `BanAccountUseCase` |
| **Directory** | Annuaire des utilisateurs visible selon les droits | `GetDirectoryQuery` |

## Patterns Utilisés

### 1. Repository Pattern
Abstraction de la persistance pour permettre de changer de BDD sans impact sur le domain.

### 2. Port & Adapter Pattern
- **Ports** : Interfaces dans le domain (`UserRepository`, `IdentityProviderPort`)
- **Adapters** : Implémentations dans l'infrastructure (`JpaUserRepository`, `KeycloakAdapter`)

### 3. Command Pattern
Encapsule les requêtes de modification :
```java
public record CreateUserCommand(
    String email,
    String firstName,
    String lastName,
    Set<String> roles
) {}
```

### 4. CQRS (Command Query Responsibility Segregation)
Séparation lecture/écriture :
- **Commands** : `CreateUserUseCase`, `UpdateUserUseCase`
- **Queries** : `GetUserQuery`, `SearchUsersQuery`

### 5. Event-Driven Architecture
Publication d'événements métier :
```java
// Publié vers RabbitMQ
UserCreatedEvent -> exchange: user.events -> queue: notification.user.created
```

### 6. Factory Pattern
Création contrôlée d'entités :
```java
User.create(command)  // vs new User(...)
```

## Tests Architecture

### ArchUnit - Validation des règles architecturales

```java
@ArchTest
static final ArchRule domain_should_not_depend_on_infrastructure = 
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..");

@ArchTest
static final ArchRule entities_should_be_in_domain = 
    classes()
        .that().areAnnotatedWith(Entity.class)
        .should().resideInAPackage("..domain.model..");

@ArchTest
static final ArchRule repositories_should_be_interfaces = 
    classes()
        .that().haveSimpleNameEndingWith("Repository")
        .and().resideInAPackage("..domain..")
        .should().beInterfaces();
```

### Pyramide de tests

```
        ╱╲
       ╱  ╲
      ╱ E2E ╲          ← 10% : Tests End-to-End avec Testcontainers
     ╱────────╲
    ╱          ╲
   ╱ Integration╲      ← 30% : Tests d'intégration (DB, Keycloak)
  ╱──────────────╲
 ╱                ╲
╱      Unit        ╲   ← 60% : Tests unitaires du domain
╲──────────────────╱
```

## Diagrammes

### Diagramme de séquence : Provisionnement d'utilisateur

```
Client        Controller    UseCase      UserRepo   OrgRepo   Keycloak   RabbitMQ
  │               │            │            │          │          │          │
  ├─Provision────→│            │            │          │          │          │
  │ +JWT          │            │            │          │          │          │
  │               ├─execute───→│            │          │          │          │
  │               │  +email    │            │          │          │          │
  │               │  +request  ├─findByEmail→          │          │          │
  │               │            │←──creator──┤          │          │          │
  │               │            │            │          │          │          │
  │               │            ├─validate permissions  │          │          │
  │               │            │            │          │          │          │
  │               │            ├─checkExists→          │          │          │
  │               │            │←──────────┤          │          │          │
  │               │            │            │          │          │          │
  │               │            ├─getOrg()───────────→│          │          │
  │               │            │←────organization─────┤          │          │
  │               │            │            │          │          │          │
  │               │            ├─User.provision()     │          │          │
  │               │            │  (domain logic)      │          │          │
  │               │            │            │          │          │          │
  │               │            ├─save()────→│          │          │          │
  │               │            │            ├─INSERT→ │          │          │
  │               │            │            │          │          │          │
  │               │            ├─provisionUser()──────────────→│          │
  │               │            │←────externalId────────────────┤          │
  │               │            │            │          │          │          │
  │               │            ├─linkToIDP()│          │          │          │
  │               │            │            │          │          │          │
  │               │            ├─publish()─────────────────────────────────→│
  │               │            │  UserProvisionedEvent│          │          │
  │               │            │            │          │          │          │
  │               │←──────────┤            │          │          │          │
  │←─201 Created──┤            │            │          │          │          │
  │ {message}     │            │            │          │          │          │
```

### Diagramme de contexte

```
┌──────────────────────────────────────────────────────────────────────┐
│                          IAM Service                                  │
│                    (Exam Center Domain)                               │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                    Presentation Layer                           │  │
│  │  ┌──────────────┐  ┌─────────────┐  ┌──────────────────────┐  │  │
│  │  │AuthController│  │AccountMgmt  │  │OnboardingController  │  │  │
│  │  └──────────────┘  └─────────────┘  └──────────────────────┘  │  │
│  └───────────────┬────────────────────────────────────────────────┘  │
│                  │                                                    │
│  ┌───────────────▼────────────────────────────────────────────────┐  │
│  │                   Application Layer                             │  │
│  │  ┌────────────┐  ┌──────────────┐  ┌─────────────────────┐    │  │
│  │  │Onboarding  │  │ProvisionUser │  │ManageAccount        │    │  │
│  │  │UseCase     │  │UseCase       │  │UseCase              │    │  │
│  │  └────────────┘  └──────────────┘  └─────────────────────┘    │  │
│  └───────────────┬────────────────────────────────────────────────┘  │
│                  │                                                    │
│  ┌───────────────▼────────────────────────────────────────────────┐  │
│  │                      Domain Layer                               │  │
│  │  ┌────────────┐  ┌──────┐  ┌──────┐  ┌─────────────────────┐  │  │
│  │  │Organization│  │ User │  │ Unit │  │Domain Events        │  │  │
│  │  │ (Aggregate)│  │(Agg.)│  │      │  │(OrganizationCreated)│  │  │
│  │  └────────────┘  └──────┘  └──────┘  └─────────────────────┘  │  │
│  └───────────────┬────────────────────────────────────────────────┘  │
│                  │                                                    │
│  ┌───────────────▼────────────────────────────────────────────────┐  │
│  │                Infrastructure Layer                             │  │
│  │  ┌───────────┐  ┌──────────────┐  ┌─────────────────────────┐ │  │
│  │  │PostgreSQL │  │  Keycloak    │  │      RabbitMQ           │ │  │
│  │  │Repository │  │  Adapter     │  │   Event Publisher       │ │  │
│  │  └───────────┘  └──────────────┘  └─────────────────────────┘ │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
           │                  │                       │
           ▼                  ▼                       ▼
    ┌────────────┐    ┌────────────┐         ┌──────────────────┐
    │  Data      │    │   Auth     │         │  Event Bus       │
    │Persistence │    │  Provider  │         │  (Async Comm.)   │
    └────────────┘    └────────────┘         └──────────────────┘
                                                      │
                              ┌───────────────────────┴──────────────────┐
                              ▼                                          ▼
                      ┌────────────────┐                      ┌─────────────────┐
                      │ Notification   │                      │  Exam Mgmt      │
                      │   Service      │                      │   Service       │
                      └────────────────┘                      └─────────────────┘
```

## Décisions Architecturales (ADR)

### ADR-001 : Choix de l'architecture hexagonale

**Contexte** : Besoin d'une architecture testable et évolutive pour un microservice critique.

**Décision** : Adoption de l'architecture hexagonale avec DDD.

**Conséquences** :
- ✅ Testabilité accrue (domain isolé)
- ✅ Changement facile des providers (Keycloak → Auth0)
- ❌ Complexité initiale plus élevée
- ❌ Besoin de formation de l'équipe

### ADR-002 : Keycloak comme Identity Provider

**Contexte** : Besoin d'un IDP open-source et standard OIDC.

**Décision** : Utilisation de Keycloak 26.0.0.

**Alternatives considérées** :
- Auth0 : Payant, moins de contrôle
- OAuth2 custom : Trop de développement

**Conséquences** :
- ✅ Standard OIDC/OAuth2
- ✅ Open-source et self-hosted
- ✅ Admin UI intégrée
- ❌ Overhead de déploiement
- ❌ Courbe d'apprentissage

### ADR-003 : RabbitMQ pour les événements

**Contexte** : Communication asynchrone entre microservices.

**Décision** : RabbitMQ comme message broker.

**Conséquences** :
- ✅ Fiabilité (acknowledgements, dead letters)
- ✅ Flexibilité du routing (exchanges)
- ❌ Complexité opérationnelle

## Évolutions futures

### Court terme
- [ ] Augmenter la couverture de tests à 80%
- [ ] Ajouter Swagger/OpenAPI documentation
- [ ] Implémenter la pagination pour les listes

### Moyen terme
- [ ] Multi-tenancy (support de plusieurs organisations)
- [ ] Audit log complet (qui a fait quoi, quand)
- [ ] Rate limiting par utilisateur

### Long terme
- [ ] Support de fédération d'identités (SAML, Google, Microsoft)
- [ ] API GraphQL en complément de REST
- [ ] Event Sourcing pour un historique complet
