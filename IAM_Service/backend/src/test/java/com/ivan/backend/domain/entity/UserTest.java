package com.ivan.backend.domain.entity;

import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Le compte doit s'activer quand l'email est vérifié par le provider")
    void should_activate_user_when_provider_verifies_email() {
        // GIVEN: Un utilisateur non vérifié et inactif
        User user = createPendingUser();
        assertFalse(user.isEmailVerified());
        assertFalse(user.isActive());

        // WHEN: On synchronise avec un statut "vérifié" venant de Keycloak
        user.syncValidationStatus(true);

        // THEN: L'utilisateur devient vérifié ET actif
        assertTrue(user.isEmailVerified());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("Le compte doit rester inactif si le provider dit que l'email n'est pas vérifié")
    void should_remain_inactive_if_provider_says_email_not_verified() {
        // GIVEN
        User user = createPendingUser();

        // WHEN
        user.syncValidationStatus(false);

        // THEN
        assertFalse(user.isEmailVerified());
        assertFalse(user.isActive());
    }

    @Test
    @DisplayName("Doit mettre à jour l'exigence de changement de mot de passe")
    void should_update_password_requirement() {
        // GIVEN
        User user = createPendingUser();
        assertFalse(user.isMustChangePassword());

        // WHEN
        user.updatePasswordRequirement(true);

        // THEN
        assertTrue(user.isMustChangePassword());
    }

    // Helper pour créer un utilisateur de base pour les tests
    private User createPendingUser() {
        return new User(
            UUID.randomUUID(),
            "Ivan",
            "D",
            new Email("test@example.com"),
            UUID.randomUUID(),
            null,
            UserRole.CENTER_OWNER,
            false, // emailVerified
            false, // isActive
            false  // mustChangePassword
        );
    }
}