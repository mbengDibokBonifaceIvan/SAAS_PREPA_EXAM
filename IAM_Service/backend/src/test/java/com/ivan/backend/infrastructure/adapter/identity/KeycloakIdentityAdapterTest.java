package com.ivan.backend.infrastructure.adapter.identity;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.exception.AccountLockedException;
import com.ivan.backend.domain.valueobject.AuthToken;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import com.ivan.backend.infrastructure.adapter.identity.exception.KeycloakIdentityException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class KeycloakIdentityAdapterTest {

        @Mock(answer = Answers.RETURNS_DEEP_STUBS)
        private Keycloak keycloak;

        @Mock
        private RestTemplate restTemplate;

        @InjectMocks
        private KeycloakIdentityAdapter adapter;

        @BeforeEach
        void setUp() {
                // Injection des @Value manuellement pour le test unitaire
                ReflectionTestUtils.setField(adapter, "realm", "ExamsRealm");
                ReflectionTestUtils.setField(adapter, "authServerUrl", "http://localhost:8080");
                ReflectionTestUtils.setField(adapter, "clientId", "iam-client");
        }

        @Test
        @DisplayName("CreateIdentity : devrait créer l'utilisateur et assigner le rôle")
        void shouldCreateIdentitySuccessfully() {
                // 1. GIVEN - Données
                User user = new User(UUID.randomUUID(), "Ivan", "Test", new Email("ivan@test.com"),
                                UUID.randomUUID(), UUID.randomUUID(), UserRole.CENTER_OWNER, false, true, true);
                String userId = "user-id-123";

                // 2. Mock de la Réponse JAX-RS (Creation)
                Response response = mock(Response.class);
                when(response.getStatus()).thenReturn(201);
                when(response.getLocation()).thenReturn(URI.create("http://keycloak/users/" + userId));

                // 3. Mock de la hiérarchie pour le Rôle
                RoleResource roleResource = mock(RoleResource.class);
                RoleRepresentation roleRep = new RoleRepresentation();
                roleRep.setName("CENTER_OWNER");
                when(keycloak.realm(anyString()).roles().get(anyString())).thenReturn(roleResource);
                when(roleResource.toRepresentation()).thenReturn(roleRep);

                // 4. FIX: Mock de la hiérarchie pour l'utilisateur (UsersResource ->
                // UserResource)
                UsersResource usersResource = mock(UsersResource.class);
                UserResource userResource = mock(UserResource.class); // C'est lui qui manquait !
                RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
                RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);

                when(keycloak.realm(anyString()).users()).thenReturn(usersResource);
                when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

                // On lie l'ID extrait au mock userResource
                when(usersResource.get(userId)).thenReturn(userResource);

                // On lie le chemin pour assignRoleToUser : .roles().realmLevel()
                when(userResource.roles()).thenReturn(roleMappingResource);
                when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

                // 5. WHEN
                assertDoesNotThrow(() -> adapter.createIdentity(user, "temp-password"));

                // 6. THEN
                verify(usersResource).create(any(UserRepresentation.class));
                verify(roleScopeResource).add(anyList()); // Vérifie l'ajout du rôle
                verify(userResource).executeActionsEmail(anyList()); // Vérifie l'envoi du mail
        }

        @Test
        @DisplayName("Authenticate : devrait retourner un AuthToken en cas de succès")
        void shouldAuthenticateSuccessfully() {
                // GIVEN
                Map<String, Object> mockResponse = Map.of(
                                "access_token", "access-123",
                                "refresh_token", "refresh-123",
                                "expires_in", 3600,
                                "token_type", "Bearer");
                when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                                .thenReturn(ResponseEntity.ok(mockResponse));

                // WHEN
                AuthToken token = adapter.authenticate("ivan@test.com", "password");

                // THEN
                assertNotNull(token);
                assertEquals("access-123", token.accessToken());
                assertEquals(3600L, token.expiresIn());
        }

        @Test
        @DisplayName("Logout : devrait appeler l'endpoint de logout")
        void shouldLogoutSuccessfully() {
                // GIVEN
                when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                                .thenReturn(ResponseEntity.ok().build());

                // WHEN & THEN
                assertDoesNotThrow(() -> adapter.logout("refresh-token-123"));
                verify(restTemplate).postForEntity(anyString(), any(), eq(Void.class));
        }

        @Test
        @DisplayName("UpdateStatus : devrait changer l'état enabled de l'utilisateur")
        void shouldDisableIdentity() {
                // GIVEN
                String email = "test@test.com";
                UserRepresentation userRep = new UserRepresentation();
                userRep.setId("uid-123");
                userRep.setEmail(email);

                when(keycloak.realm(anyString()).users().searchByEmail(email, true))
                                .thenReturn(List.of(userRep));

                // WHEN
                adapter.disableIdentity(email);

                // THEN
                verify(keycloak.realm(anyString()).users().get("uid-123")).update(any(UserRepresentation.class));
                assertFalse(userRep.isEnabled());
        }

        @Test
        @DisplayName("CreateIdentity : devrait lancer KeycloakIdentityException si status 409")
        void shouldThrowKeycloakIdentityException_WhenKeycloakReturns409() {
                // GIVEN
                User user = new User(UUID.randomUUID(), "Ivan", "Test", new Email("duplicate@test.com"),
                                UUID.randomUUID(), UUID.randomUUID(), UserRole.CENTER_OWNER, false, true, true);

                Response response = mock(Response.class);
                when(response.getStatus()).thenReturn(409); // Simule le conflit

                UsersResource usersResource = mock(UsersResource.class);
                when(keycloak.realm(anyString()).users()).thenReturn(usersResource);
                when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

                // WHEN & THEN
                assertThrows(KeycloakIdentityException.class, () -> adapter.createIdentity(user, "password"));
        }

        @Test
        @DisplayName("Authenticate : devrait lancer AccountLockedException si le compte est désactivé ou verrouillé")
        void shouldThrowAccountLockedException_WhenUserIsDisabledInKeycloak() {
                // 1. Simuler une erreur 401 de RestTemplate
                HttpClientErrorException authException = mock(HttpClientErrorException.class);
                when(authException.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
                when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenThrow(authException);

                // 2. Mock de la recherche utilisateur
                UserRepresentation userRep = new UserRepresentation();
                userRep.setId("uid-123");
                userRep.setEnabled(false); // Utilisateur désactivé
                when(keycloak.realm(anyString()).users().searchByEmail(anyString(), anyBoolean()))
                                .thenReturn(List.of(userRep));

                // 3. Mock du Brute Force Status
                Map<String, Object> bruteStatus = Map.of("disabled", false);
                when(keycloak.realm(anyString()).attackDetection().bruteForceUserStatus("uid-123"))
                                .thenReturn(bruteStatus);

                // WHEN & THEN
                assertThrows(AccountLockedException.class, () -> adapter.authenticate("ivan@test.com", "wrong-pass"));
        }

        @Test
        @DisplayName("UpdateUserRole : devrait supprimer les anciens rôles et ajouter le nouveau")
        void shouldUpdateUserRole_ByRemovingOldRolesFirst() {
                // GIVEN
                String email = "update@test.com";
                String newRole = "UNIT_MANAGER";
                String oldRole = "CANDIDATE";

                UserRepresentation userRep = new UserRepresentation();
                userRep.setId("uid-123");
                userRep.setAttributes(new java.util.HashMap<>()); // Nécessaire pour le put() final

                RoleRepresentation oldRoleRep = new RoleRepresentation();
                oldRoleRep.setName(oldRole);

                RoleRepresentation newRoleRep = new RoleRepresentation();
                newRoleRep.setName(newRole);

                UserResource userResource = mock(UserResource.class, Answers.RETURNS_DEEP_STUBS);

                when(keycloak.realm(anyString()).users().searchByEmail(email, true)).thenReturn(List.of(userRep));
                when(keycloak.realm(anyString()).users().get("uid-123")).thenReturn(userResource);

                // Simuler que l'utilisateur a déjà l'ancien rôle
                when(userResource.roles().realmLevel().listAll()).thenReturn(List.of(oldRoleRep));
                when(keycloak.realm(anyString()).roles().get(newRole).toRepresentation()).thenReturn(newRoleRep);

                // WHEN
                adapter.updateUserRole(email, newRole);

                // THEN
                verify(userResource.roles().realmLevel()).remove(anyList()); // Vérifie la suppression
                verify(userResource.roles().realmLevel()).add(anyList()); // Vérifie l'ajout
                assertEquals(newRole, userRep.getAttributes().get("role").get(0));
        }

        @Test
        @DisplayName("CreateIdentity : devrait lancer une exception si le header Location est absent")
        void shouldThrowException_WhenLocationHeaderIsMissing() {
                // GIVEN
                User user = new User(UUID.randomUUID(), "Ivan", "Test", new Email("missing@loc.com"),
                                UUID.randomUUID(), UUID.randomUUID(), UserRole.CENTER_OWNER, false, false, false);

                Response response = mock(Response.class);
                when(response.getStatus()).thenReturn(201);
                when(response.getLocation()).thenReturn(null);

                UsersResource usersResource = mock(UsersResource.class);
                when(keycloak.realm(anyString()).users()).thenReturn(usersResource);
                when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

                // WHEN
                KeycloakIdentityException ex = assertThrows(KeycloakIdentityException.class,
                                () -> adapter.createIdentity(user, "pass"));

                // THEN
                // On vérifie le message du wrapper ou de la cause
                String fullMessage = ex.getMessage() + (ex.getCause() != null ? ex.getCause().getMessage() : "");
                assertTrue(fullMessage.contains("Location header manquant"),
                                "L'exception devrait mentionner le manque du header Location (Message actuel: "
                                                + fullMessage + ")");
        }

        @Test
        @DisplayName("getStatus : devrait lancer une exception si l'utilisateur n'existe pas")
        void shouldThrowException_WhenUserNotFoundInGetStatus() {
                // GIVEN
                String email = "unknown@test.com";
                when(keycloak.realm(anyString()).users().searchByEmail(email, true))
                                .thenReturn(Collections.emptyList()); // Liste vide

                // WHEN & THEN
                assertThrows(KeycloakIdentityException.class, () -> adapter.getStatus(email));
        }

        @Test
        @DisplayName("CreateIdentity : devrait lancer une exception pour un code erreur non géré (ex: 500)")
        void shouldThrowException_WhenKeycloakReturnsOtherError() {
                // GIVEN
                User user = new User(UUID.randomUUID(), "Ivan", "Test", new Email("error@test.com"),
                                UUID.randomUUID(), UUID.randomUUID(), UserRole.CENTER_OWNER, false, false, false);

                Response response = mock(Response.class);
                when(response.getStatus()).thenReturn(500);
                when(response.getStatusInfo()).thenReturn(mock(jakarta.ws.rs.core.Response.StatusType.class));

                UsersResource usersResource = mock(UsersResource.class);
                when(keycloak.realm(anyString()).users()).thenReturn(usersResource);
                when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

                // WHEN & THEN
                assertThrows(KeycloakIdentityException.class, () -> adapter.createIdentity(user, "pass"));
        }

        @Test
        @DisplayName("Authenticate : devrait lancer une exception si le corps de la réponse est nul")
        void shouldThrowException_WhenAuthResponseBodyIsNull() {
                // GIVEN
                when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                                .thenReturn(ResponseEntity.ok(null)); // Simule un body vide

                // WHEN & THEN
                KeycloakIdentityException ex = assertThrows(KeycloakIdentityException.class,
                                () -> adapter.authenticate("test@test.com", "password"));
                assertTrue(ex.getMessage().contains("Réponse Keycloak vide"));
        }
}