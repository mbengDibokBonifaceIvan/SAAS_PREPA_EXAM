package com.ivan.backend.infrastructure.adapter.identity;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;

@ExtendWith(MockitoExtension.class)
class KeycloakIdentityAdapterTest {

    @Mock
    private Keycloak keycloak;

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

        // On simule la cascade d'appels : keycloak.realm("...").users()
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    @Test
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
                false);

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

}