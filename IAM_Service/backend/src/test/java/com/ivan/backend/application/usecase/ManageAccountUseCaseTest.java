package com.ivan.backend.application.usecase;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.port.out.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ManageAccountUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private MessagePublisherPort messagePublisher;
    @Mock private IdentityManagerPort identityManagerPort;

    @InjectMocks
    private ManageAccountUseCase manageAccountUseCase;

    @Test
    @DisplayName("Bannissement : devrait désactiver le compte localement et sur Keycloak")
    void shouldBanAccountSuccessfully() {
        // GIVEN
        String targetEmail = "user@test.com";
        String adminEmail = "admin@test.com";
        
        // On mocke les deux utilisateurs
        User target = mock(User.class);
        User requester = mock(User.class);

        when(userRepository.findByEmail(new Email(targetEmail))).thenReturn(Optional.of(target));
        when(userRepository.findByEmail(new Email(adminEmail))).thenReturn(Optional.of(requester));

        // WHEN
        manageAccountUseCase.banAccount(targetEmail, adminEmail);

        // THEN
        verify(requester).checkCanManage(target); // Vérifie la sécurité
        verify(target).deactivate();              // Vérifie l'action domaine
        verify(userRepository).save(target);       // Vérifie la persistence
        verify(identityManagerPort).disableIdentity(targetEmail); // Vérifie l'action Keycloak
        verify(messagePublisher).publishAccountBanned(any());     // Vérifie la notification RabbitMQ
    }

    @Test
    @DisplayName("Devrait lever une exception si l'utilisateur cible n'existe pas")
    void shouldThrowExceptionWhenUserNotFound() {
        // GIVEN
        String unknownEmail = "unknown@test.com";
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> {
            manageAccountUseCase.banAccount(unknownEmail, "admin@test.com");
        });

        // On vérifie qu'on s'est arrêté dès le début
        verifyNoInteractions(identityManagerPort, messagePublisher);
    }
}