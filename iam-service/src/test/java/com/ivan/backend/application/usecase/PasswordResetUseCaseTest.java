package com.ivan.backend.application.usecase;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.event.PasswordResetRequestedEvent;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.port.out.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class PasswordResetUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private IdentityManagerPort identityManagerPort;
    @Mock private MessagePublisherPort messagePublisher;

    @InjectMocks
    private PasswordResetUseCase passwordResetUseCase;

    @Test
    @DisplayName("Reset : devrait envoyer le mail si l'utilisateur existe et est actif")
    void shouldRequestReset_WhenUserExistsAndIsActive() {
        // GIVEN
        String email = "active@test.com";
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(userRepository.findByEmail(new Email(email))).thenReturn(Optional.of(user));

        // WHEN
        passwordResetUseCase.requestReset(email);

        // THEN
        verify(identityManagerPort, times(1)).sendPasswordReset(email);
        verify(messagePublisher, times(1)).publishPasswordResetRequested(any(PasswordResetRequestedEvent.class));
    }

    @Test
    @DisplayName("Reset : ne devrait rien faire si l'utilisateur est banni (inactif)")
    void shouldDoNothing_WhenUserIsBanned() {
        // GIVEN
        String email = "banned@test.com";
        User user = mock(User.class);
        when(user.isActive()).thenReturn(false); // Utilisateur banni
        when(userRepository.findByEmail(new Email(email))).thenReturn(Optional.of(user));

        // WHEN
        passwordResetUseCase.requestReset(email);

        // THEN
        verifyNoInteractions(identityManagerPort, messagePublisher);
    }

    @Test
    @DisplayName("Reset : ne devrait rien faire si l'utilisateur n'existe pas (Sécurité)")
    void shouldDoNothing_WhenUserDoesNotExist() {
        // GIVEN
        String email = "unknown@test.com";
        when(userRepository.findByEmail(new Email(email))).thenReturn(Optional.empty());

        // WHEN
        passwordResetUseCase.requestReset(email);

        // THEN
        // On vérifie qu'aucune exception n'est jetée et qu'aucun service n'est appelé
        verifyNoInteractions(identityManagerPort, messagePublisher);
    }
}