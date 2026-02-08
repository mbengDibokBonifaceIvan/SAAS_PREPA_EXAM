package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.LoginRequest;
import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.event.AccountLockedEvent;
import com.ivan.backend.domain.exception.AccountLockedException;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.port.out.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.AuthToken;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.ProviderStatus; // Import mis à jour
import com.ivan.backend.domain.valueobject.UserRole;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @Mock
    private IdentityManagerPort identityManagerPort;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessagePublisherPort messagePublisher;

    @InjectMocks
    private LoginUseCase loginUseCase;

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

        // Simule Keycloak : Email vérifié (true) et pas de changement de mot de passe
        // requis (false)
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

    @Test
    @DisplayName("Login : devrait gérer le verrouillage de compte et notifier le propriétaire")
    void should_handle_account_lockout() {
        // GIVEN
        LoginRequest request = new LoginRequest("ivan@test.com", "wrong-password");
        Email email = new Email("ivan@test.com");

        // Simule l'exception de l'IAM
        when(identityManagerPort.authenticate(any(), any()))
                .thenThrow(new AccountLockedException("Compte verrouillé après 3 tentatives"));

        // Simule la présence de l'utilisateur et de son Owner de centre
        User owner = new User(UUID.randomUUID(), "Boss", "D", new Email("boss@test.com"),
                testUser.getTenantId(), null, UserRole.CENTER_OWNER, true, true, false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userRepository.findByRoleAndTenantId(UserRole.CENTER_OWNER, testUser.getTenantId()))
                .thenReturn(Optional.of(owner));

        // WHEN & THEN
        assertThrows(AccountLockedException.class, () -> loginUseCase.login(request));

        // Vérifie la logique métier de verrouillage
        assertFalse(testUser.isActive(), "L'utilisateur local doit être désactivé");
        verify(userRepository).save(testUser);

        // Vérifie l'envoi de l'événement de sécurité
        verify(messagePublisher).publishAccountLocked(any(AccountLockedEvent.class));
    }

    @Test
    @DisplayName("Login : devrait échouer si l'utilisateur n'existe pas localement")
    void should_throw_exception_when_user_not_found_locally() {
        // GIVEN
        LoginRequest request = new LoginRequest("ghost@test.com", "password123");
        AuthToken fakeToken = new AuthToken("access", "refresh", 3600L, "Bearer");

        when(identityManagerPort.authenticate(any(), any())).thenReturn(fakeToken);
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // WHEN & THEN
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> loginUseCase.login(request));

        assertTrue(ex.getMessage().contains("non synchronisé localement"));
        verify(userRepository, never()).save(any());
    }
}