package com.ivan.backend.infrastructure.adapter.persistence;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaUserRepositoryAdapter.class)
// Force le scan si tes entités/repos sont dans des packages différents
@EntityScan(basePackageClasses = UserEntity.class) 
@EnableJpaRepositories(basePackageClasses = SpringDataUserRepository.class)
class JpaUserRepositoryAdapterTest {

    @Autowired
    private JpaUserRepositoryAdapter adapter;

    @Test
    void should_save_and_retrieve_user_from_database() {
        // Given: Un utilisateur du domaine
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Email email = new Email("test@ivan.com");
        
        User user = new User(
            userId, 
            "Ivan", 
            "D", 
            email, 
            tenantId, 
            UserRole.CENTER_OWNER
        );

        // When: On sauvegarde via l'adaptateur
        adapter.save(user);

        // Then: On récupère et on vérifie la correspondance
        var result = adapter.findByEmail(email);

        assertTrue(result.isPresent(), "L'utilisateur devrait être trouvé en base");
        result.ifPresent(foundUser -> {
            assertEquals(userId, foundUser.getId());
            assertEquals("Ivan", foundUser.getFirstName());
            assertEquals("test@ivan.com", foundUser.getEmail().value());
            assertEquals(tenantId, foundUser.getTenantId());
            assertEquals(UserRole.CENTER_OWNER, foundUser.getRole());
        });
    }
}