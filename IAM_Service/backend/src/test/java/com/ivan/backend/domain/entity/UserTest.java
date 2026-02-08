package com.ivan.backend.domain.entity;

import com.ivan.backend.domain.exception.BusinessRuleViolationException;
import com.ivan.backend.domain.exception.InsufficientPrivilegesException;
import com.ivan.backend.domain.exception.ResourceAccessDeniedException;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Nested;

@ActiveProfiles("test")
class UserTest {

    private final UUID tenant1 = UUID.randomUUID();
    private final UUID tenant2 = UUID.randomUUID();
    private final UUID unit1 = UUID.randomUUID();
    private final UUID unit2 = UUID.randomUUID();

    @Nested
    @DisplayName("Vérification des droits (checkCanManage)")
    class CheckCanManageTests {

        @Test
        @DisplayName("Devrait rejeter si les centres (tenants) sont différents")
        void shouldRejectDifferentTenants() {
            User requester = createUser(tenant1, unit1, UserRole.CENTER_OWNER);
            User target = createUser(tenant2, unit1, UserRole.CANDIDATE);

            assertThrows(ResourceAccessDeniedException.class, () -> requester.checkCanManage(target));
        }

        @Test
        @DisplayName("Un manager ne devrait pas pouvoir gérer un autre manager (rang égal)")
        void shouldRejectEqualRole() {
            User manager1 = createUser(tenant1, unit1, UserRole.UNIT_MANAGER);
            User manager2 = createUser(tenant1, unit1, UserRole.UNIT_MANAGER);

            assertThrows(InsufficientPrivilegesException.class, () -> manager1.checkCanManage(manager2));
        }

        @Test
        @DisplayName("Un manager ne devrait pas pouvoir gérer quelqu'un d'une autre unité")
        void shouldRejectDifferentUnitForManager() {
            User manager = createUser(tenant1, unit1, UserRole.UNIT_MANAGER);
            User target = createUser(tenant1, unit2, UserRole.CANDIDATE);

            assertThrows(ResourceAccessDeniedException.class, () -> manager.checkCanManage(target));
        }

        @Test
        @DisplayName("L'Owner du centre peut gérer n'importe qui dans son centre, peu importe l'unité")
        void shouldAllowOwnerEverywhereInTenant() {
            User owner = createUser(tenant1, unit1, UserRole.CENTER_OWNER);
            User target = createUser(tenant1, unit2, UserRole.CANDIDATE);

            assertDoesNotThrow(() -> owner.checkCanManage(target));
        }

        @Test
        @DisplayName("Un utilisateur peut toujours se gérer lui-même")
        void shouldAllowSelfManagement() {
            User user = createUser(tenant1, unit1, UserRole.CANDIDATE);
            assertDoesNotThrow(() -> user.checkCanManage(user));
        }
    }

    @Nested
    @DisplayName("Mises à jour du profil")
    class ProfileTests {
        @Test
        @DisplayName("Devrait mettre à jour le nom et prénom si valides")
        void shouldUpdateProfile() {
            User user = createUser(tenant1, unit1, UserRole.CANDIDATE);
            user.updateProfile("Jean", "Dupont");

            assertEquals("Jean", user.getFirstName());
            assertEquals("Dupont", user.getLastName());
        }

        @Test
        @DisplayName("Devrait échouer si le prénom est vide")
        void shouldFailWhenFirstNameIsEmpty() {
            User user = createUser(tenant1, unit1, UserRole.CANDIDATE);
            assertThrows(BusinessRuleViolationException.class, () -> user.updateProfile("", "Dupont"));
        }
    }

    @Nested
    @DisplayName("Création d'utilisateurs (validateCanCreate)")
    class CreationTests {

        @Test
        @DisplayName("Devrait rejeter si l'on crée pour un autre tenant")
        void shouldRejectDifferentTenantOnCreation() {
            User owner = createUser(tenant1, unit1, UserRole.CENTER_OWNER);
            assertThrows(ResourceAccessDeniedException.class,
                    () -> owner.validateCanCreate(UserRole.STAFF_MEMBER, tenant2, unit1));
        }

        @Test
        @DisplayName("Devrait rejeter si le rôle cible est supérieur ou égal")
        void shouldRejectHigherOrEqualRoleCreation() {
            User manager = createUser(tenant1, unit1, UserRole.UNIT_MANAGER);
            // Un manager ne peut pas créer un autre manager ou un owner
            assertThrows(InsufficientPrivilegesException.class,
                    () -> manager.validateCanCreate(UserRole.UNIT_MANAGER, tenant1, unit1));
        }

        @Test
        @DisplayName("Devrait rejeter si un manager crée hors de son unité")
        void shouldRejectDifferentUnitOnCreationForManager() {
            User manager = createUser(tenant1, unit1, UserRole.UNIT_MANAGER);
            assertThrows(ResourceAccessDeniedException.class,
                    () -> manager.validateCanCreate(UserRole.CANDIDATE, tenant1, unit2));
        }

        @Test
        @DisplayName("L'owner peut créer dans n'importe quelle unité du centre")
        void shouldAllowOwnerToCreateInAnyUnit() {
            User owner = createUser(tenant1, unit1, UserRole.CENTER_OWNER);
            assertDoesNotThrow(() -> owner.validateCanCreate(UserRole.CANDIDATE, tenant1, unit2));
        }
    }

    @Nested
    @DisplayName("Actions d'état et modifications")
    class StatusAndAssignmentTests {

        @Test
        @DisplayName("Devrait désactiver et activer le compte")
        void shouldToggleActivation() {
            User user = createUser(tenant1, unit1, UserRole.CANDIDATE);

            user.deactivate();
            assertFalse(user.isActive());

            user.activate();
            assertTrue(user.isActive());
        }

        @Test
        @DisplayName("Devrait assigner à une nouvelle unité")
        void shouldAssignToUnit() {
            User user = createUser(tenant1, unit1, UserRole.CANDIDATE);
            UUID newUnitId = UUID.randomUUID();

            user.assignToUnit(newUnitId);
            assertEquals(newUnitId, user.getUnitId());
        }

        @Test
        @DisplayName("Devrait changer le rôle si le demandeur est différent")
        void shouldChangeRoleSuccessfully() {
            User target = createUser(tenant1, unit1, UserRole.CANDIDATE);
            UUID requesterId = UUID.randomUUID();

            target.changeRole(UserRole.STAFF_MEMBER, requesterId);
            assertEquals(UserRole.STAFF_MEMBER, target.getRole());
        }
    }

    @Test
    @DisplayName("Constructeur : devrait générer un ID si fourni null")
    void shouldGenerateIdWhenNullProvided() {
        User user = new User(null, "Ivan", "D", new Email("test@test.com"),
                tenant1, unit1, UserRole.CANDIDATE, true, true, false);

        assertNotNull(user.getId());
    }

    @Test
    @DisplayName("Sécurité : un utilisateur ne peut pas changer son propre rôle")
    void shouldNotAllowSelfRoleChange() {
        User owner = createUser(tenant1, unit1, UserRole.CENTER_OWNER);
        UUID ownerId = owner.getId(); // On prépare l'ID à l'extérieur

        assertThrows(InsufficientPrivilegesException.class, () -> owner.changeRole(UserRole.CANDIDATE, ownerId));
    }

    // Helper pour créer rapidement un utilisateur
    private User createUser(UUID tenantId, UUID unitId, UserRole role) {
        return new User(
                UUID.randomUUID(), "Test", "User", new Email("test@test.com"),
                tenantId, unitId, role, true, true, false);
    }

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
                false // mustChangePassword
        );
    }

}