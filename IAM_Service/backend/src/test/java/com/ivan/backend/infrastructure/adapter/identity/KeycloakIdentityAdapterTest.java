package com.ivan.backend.infrastructure.adapter.identity;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.AuthToken;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.ProviderStatus;
import com.ivan.backend.domain.valueobject.UserRole;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

@ExtendWith(MockitoExtension.class)
class KeycloakIdentityAdapterTest {

    @Mock
    private Keycloak keycloak;
    
    @Mock 
    private RestTemplate restTemplate;
    
    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @InjectMocks
    private KeycloakIdentityAdapter adapter;

    @BeforeEach
    void setup() {
        // On injecte manuellement la valeur de la propriété @Value("${keycloak.realm}")
        ReflectionTestUtils.setField(adapter, "realm", "ExamsRealm");
ReflectionTestUtils.setField(adapter, "clientId", "iam-client");
        ReflectionTestUtils.setField(adapter, "clientSecret", "secret");

        // On utilise lenient() pour éviter l'exception dans le test d'authentification
        // On simule la cascade d'appels : keycloak.realm("...").users()
        lenient().when(keycloak.realm(anyString())).thenReturn(realmResource);
        lenient().when(realmResource.users()).thenReturn(usersResource);
    }

    @Test
    @DisplayName("Devrait créer une identité dans Keycloak et envoyer l'email de vérification")
    void should_call_keycloak_api_to_create_user() {
        // GIVEN
        User user = new User(
                UUID.randomUUID(),
                "Ivan",
                "D",
                new Email("ivan@test.com"),
                UUID.randomUUID(),
                null,
                UserRole.CENTER_OWNER,
                false,
                false,
                true
            
            );

        // CRUCIAL : URI retournée par Keycloak dans le header Location
        URI userUri = URI.create(
                "http://localhost:8080/admin/realms/ExamsRealm/users/" + UUID.randomUUID());

        Response mockResponse = Response.status(Response.Status.CREATED)
                .location(userUri)
                .build();

        when(usersResource.create(any())).thenReturn(mockResponse);

        // On mock également l'appel sendVerifyEmail et l'assignation de rôle pour éviter d'autres NPE
        UserResource userResourceMock = mock(UserResource.class);
        RoleMappingResource roleMappingResourceMock = mock(RoleMappingResource.class);
        RoleScopeResource roleScopeResourceMock = mock(RoleScopeResource.class);
        RoleResource roleResourceMock = mock(RoleResource.class);
        RolesResource rolesResourceMock = mock(RolesResource.class);

        when(usersResource.get(anyString())).thenReturn(userResourceMock);
        when(userResourceMock.roles()).thenReturn(roleMappingResourceMock);
        when(roleMappingResourceMock.realmLevel()).thenReturn(roleScopeResourceMock);

        // Mock pour la récupération du rôle
        when(realmResource.roles()).thenReturn(rolesResourceMock);
        when(rolesResourceMock.get(anyString())).thenReturn(roleResourceMock);
        when(roleResourceMock.toRepresentation()).thenReturn(new RoleRepresentation());

        // WHEN & THEN
        assertDoesNotThrow(() -> adapter.createIdentity(user, "password123"));

        verify(usersResource, times(1)).create(any());
        verify(userResourceMock, times(1)).sendVerifyEmail();
    }

    @Test
    @DisplayName("Devrait retourner un token valide lors d'une authentification réussie")
    void should_return_auth_token_on_successful_authentication() {
        // GIVEN
        Map<String, Object> mockResponseBody = Map.of(
            "access_token", "fake-jwt-token",
            "refresh_token", "fake-refresh-token",
            "expires_in", 3600,
            "token_type", "Bearer"
        );

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockResponseBody));

        // WHEN
        AuthToken result = adapter.authenticate("ivan@test.com", "password123");

        // THEN
        assertNotNull(result);
        assertEquals("fake-jwt-token", result.accessToken());
        assertEquals(3600L, result.expiresIn());
        verify(restTemplate).postForEntity(contains("/protocol/openid-connect/token"), any(), eq(Map.class));
    }

    @Test
    @DisplayName("Devrait récupérer le statut de vérification et de changement de mot de passe")
    void should_return_provider_status_from_keycloak() {
        // GIVEN
        String email = "ivan@test.com";
        UserRepresentation kUserRep = new UserRepresentation();
        kUserRep.setEmailVerified(true);
        kUserRep.setRequiredActions(List.of("UPDATE_PASSWORD"));

        when(usersResource.searchByEmail(email, true)).thenReturn(List.of(kUserRep));

        // WHEN
        ProviderStatus status = adapter.getStatus(email);

        // THEN
        assertTrue(status.isEmailVerified(), "L'email devrait être marqué comme vérifié");
        assertTrue(status.mustChangePassword(), "L'utilisateur devrait avoir une action UPDATE_PASSWORD");
        verify(usersResource).searchByEmail(email, true);
    }


}