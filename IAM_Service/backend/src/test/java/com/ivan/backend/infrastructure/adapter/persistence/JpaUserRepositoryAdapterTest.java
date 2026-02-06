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
        // On vérifie que l'auditeur est bien celui par défaut (SYSTEM) quand pas de session
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
                tenantId, unitId, role, false, true, true
        );
    }
}