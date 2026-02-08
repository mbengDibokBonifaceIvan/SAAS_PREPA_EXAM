package com.ivan.backend.infrastructure.adapter.persistence;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import com.ivan.backend.infrastructure.config.PersistenceConfig;
import com.ivan.backend.infrastructure.config.SecurityAuditorAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        JpaUserRepositoryAdapter.class,
        PersistenceConfig.class,
        SecurityAuditorAware.class
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // Ajoute ceci
class JpaUserRepositoryAdapterTest {

    @Autowired
    private JpaUserRepositoryAdapter adapter;

    @Autowired
    private SpringDataUserRepository springRepository;

    private UUID tenantId;
    private UUID unitId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        unitId = UUID.randomUUID();
        springRepository.deleteAll();
    }

    @Test
    @DisplayName("Save & FindById : devrait persister et récupérer l'utilisateur")
    void shouldSaveAndFindUser() {
        User user = createSampleUser("ivan@test.com", UserRole.CANDIDATE);
        adapter.save(user);

        Optional<User> found = adapter.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail().value()).isEqualTo("ivan@test.com");
    }

    @Test
    @DisplayName("Save : devrait remplir les champs d'audit automatiquement")
    void shouldHandleAuditingFields() {
        User user = createSampleUser("audit@test.com", UserRole.CANDIDATE);

        adapter.save(user);

        UserModel entity = springRepository.findById(user.getId()).orElseThrow();
        assertThat(entity.getCreatedAt()).isNotNull();
        // On vérifie que l'auditeur est bien celui par défaut (SYSTEM) quand pas de
        // session
        assertThat(entity.getCreatedBy()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("FindAllByTenantId : devrait filtrer les utilisateurs par organisation")
    void shouldFindAllByTenantId() {
        adapter.save(createSampleUser("u1@test.com", UserRole.CANDIDATE));
        adapter.save(createSampleUser("u2@test.com", UserRole.CANDIDATE));

        List<User> results = adapter.findAllByTenantId(tenantId);

        assertThat(results).hasSize(2);
    }

    private User createSampleUser(String email, UserRole role) {
        return new User(
                UUID.randomUUID(), "Ivan", "Test", new Email(email),
                tenantId, unitId, role, false, true, true);
    }

    @Test
    @DisplayName("Save : devrait mettre à jour un utilisateur existant au lieu d'en créer un nouveau")
    void shouldUpdateExistingUser() {
        // GIVEN
        User user = createSampleUser("update@test.com", UserRole.CANDIDATE);
        adapter.save(user);

        // modification du nom dans le domaine
        user.updateProfile("NouveauPrenom", "NouveauNom");

        // WHEN
        adapter.save(user);

        // THEN
        Optional<User> updated = adapter.findById(user.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getFirstName()).isEqualTo("NouveauPrenom");
        assertThat(springRepository.count()).isEqualTo(1); // On vérifie qu'il n'y a toujours qu'une ligne
    }

    @Test
    @DisplayName("FindByRoleAndTenantId : devrait trouver l'owner spécifique d'un centre")
    void shouldFindByRoleAndTenant() {
        adapter.save(createSampleUser("owner@test.com", UserRole.CENTER_OWNER));
        adapter.save(createSampleUser("candidate@test.com", UserRole.CANDIDATE));

        Optional<User> result = adapter.findByRoleAndTenantId(UserRole.CENTER_OWNER, tenantId);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail().value()).isEqualTo("owner@test.com");
    }

    @Test
    @DisplayName("FindAllByUnitId : devrait filtrer correctement par unité")
    void shouldFindAllByUnitId() {
        UUID otherUnit = UUID.randomUUID();
        adapter.save(createSampleUser("unit1@test.com", UserRole.CANDIDATE)); // unitId global

        // Un utilisateur dans une autre unité
        User userOtherUnit = new User(UUID.randomUUID(), "Other", "User", new Email("other@test.com"),
                tenantId, otherUnit, UserRole.CANDIDATE, false, true, true);
        adapter.save(userOtherUnit);

        List<User> results = adapter.findAllByUnitId(unitId);
        List<User> resultsOther = adapter.findAllByUnitId(otherUnit);

        assertThat(results).hasSize(1);
        assertThat(resultsOther).hasSize(1);
    }

    @Test
    @DisplayName("FindAllByUnitIdAndTenantId : devrait croiser les deux filtres")
    void shouldFindAllByUnitAndTenant() {
        UUID otherTenant = UUID.randomUUID();
        adapter.save(createSampleUser("good@test.com", UserRole.CANDIDATE)); // unitId + tenantId

        // Même unité mais autre centre
        User userOtherTenant = new User(UUID.randomUUID(), "Other", "Tenant", new Email("wrong@test.com"),
                otherTenant, unitId, UserRole.CANDIDATE, false, true, true);
        adapter.save(userOtherTenant);

        List<User> results = adapter.findAllByUnitIdAndTenantId(unitId, tenantId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail().value()).isEqualTo("good@test.com");
    }

    @Test
    @DisplayName("FindByEmail : devrait retourner un Optional vide si l'email n'existe pas")
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> result = adapter.findByEmail(new Email("unknown@test.com"));
        assertThat(result).isEmpty();
    }
}