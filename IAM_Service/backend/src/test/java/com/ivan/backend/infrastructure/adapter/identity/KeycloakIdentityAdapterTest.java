package com.ivan.backend.infrastructure.adapter.identity;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
            UserRole.CENTER_OWNER
        );

        // Simulation d'une réponse HTTP 201 Created de Keycloak
        Response mockResponse = Response.status(Response.Status.CREATED).build();
        when(usersResource.create(any())).thenReturn(mockResponse);

        // WHEN & THEN
        assertDoesNotThrow(() -> adapter.createIdentity(user, "password123"));
        
        // Vérification que la méthode create a bien été appelée une fois
        verify(usersResource, times(1)).create(any());
    }
}