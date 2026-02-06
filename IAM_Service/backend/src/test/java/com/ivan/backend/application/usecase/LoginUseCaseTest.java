package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.LoginRequest;
import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.AuthToken;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.ProviderStatus; // Import mis à jour
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock private IdentityManagerPort identityManagerPort;
    @Mock private UserRepository userRepository;

    @InjectMocks private LoginUseCase loginUseCase;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(
            UUID.randomUUID(), "Ivan", "D", new Email("ivan@test.com"),
            UUID.randomUUID(), null, UserRole.CENTER_OWNER, 
            false, false, false // Email non vérifié, Inactif, Password OK
        );
    }

    @Test
    void should_synchronize_and_activate_user_on_successful_login() {
        // GIVEN
        LoginRequest request = new LoginRequest("ivan@test.com", "password123");
        AuthToken fakeToken = new AuthToken("access", "refresh", 3600L, "Bearer");
        
        // Simule Keycloak : Email vérifié (true) et pas de changement de mot de passe requis (false)
        ProviderStatus providerStatus = new ProviderStatus(true, false);

        when(identityManagerPort.authenticate(any(), any())).thenReturn(fakeToken);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
        when(identityManagerPort.getStatus(any())).thenReturn(providerStatus);

        // WHEN
        LoginResponse response = loginUseCase.login(request);

        // THEN
        assertTrue(testUser.isEmailVerified(), "L'utilisateur local doit être marqué comme vérifié");
        assertTrue(testUser.isActive(), "L'utilisateur local doit être activé automatiquement");
        assertFalse(testUser.isMustChangePassword(), "L'utilisateur ne devrait pas avoir à changer de MDP");
        
        verify(userRepository, times(1)).save(testUser); // Vérifie la persistance
        assertEquals("ivan@test.com", response.email());
        assertEquals("access", response.accessToken());
    }
}