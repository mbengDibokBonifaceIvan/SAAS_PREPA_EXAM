package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.ProvisionUserRequest;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.event.UserProvisionedEvent;
import com.ivan.backend.domain.exception.UserAlreadyExistsException;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.port.out.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

import jakarta.persistence.EntityNotFoundException;
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
class ProvisionUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private IdentityManagerPort identityManagerPort;
    @Mock private MessagePublisherPort messagePublisher;

    @InjectMocks
    private ProvisionUserUseCase provisionUserUseCase;

    @Test
    @DisplayName("Provisioning : succès quand le créateur a les droits")
    void shouldProvisionUserSuccessfully() {
        // GIVEN
        String creatorEmail = "admin@tenant.com";
        UUID tenantId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        
        ProvisionUserRequest request = new ProvisionUserRequest(
                "New", "User", "new@test.com", UserRole.CANDIDATE, unitId
        );

        User creator = mock(User.class);
        when(creator.getTenantId()).thenReturn(tenantId);

        when(userRepository.findByEmail(new Email("new@test.com"))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(new Email(creatorEmail))).thenReturn(Optional.of(creator));

        // WHEN
        provisionUserUseCase.execute(request, creatorEmail);

        // THEN
        // Vérifie la validation des droits
        verify(creator).validateCanCreate(UserRole.CANDIDATE, tenantId, unitId);
        
        // Vérifie les appels d'infrastructure
        verify(identityManagerPort).createIdentity(any(User.class), anyString());
        verify(userRepository).save(any(User.class));
        verify(messagePublisher).publishUserProvisioned(any(UserProvisionedEvent.class));
    }

    @Test
    @DisplayName("Provisioning : devrait échouer si l'email cible existe déjà")
    void shouldThrowExceptionWhenUserAlreadyExists() {
        // GIVEN
        String targetEmail = "existing@test.com";
        ProvisionUserRequest request = new ProvisionUserRequest("A", "B", targetEmail, UserRole.CANDIDATE, UUID.randomUUID());
        
        when(userRepository.findByEmail(new Email(targetEmail))).thenReturn(Optional.of(mock(User.class)));

        // WHEN & THEN
        assertThrows(UserAlreadyExistsException.class, () -> provisionUserUseCase.execute(request, "any@mail.com"));
        
        verify(userRepository, times(1)).findByEmail(any(Email.class));
        verifyNoMoreInteractions(identityManagerPort, messagePublisher);
    }

    @Test
    @DisplayName("Provisioning : devrait échouer si le créateur est inconnu")
    void shouldThrowExceptionWhenCreatorNotFound() {
        // GIVEN
        String creatorEmail = "ghost@test.com";
        ProvisionUserRequest request = new ProvisionUserRequest("A", "B", "new@test.com", UserRole.CANDIDATE, UUID.randomUUID());

        when(userRepository.findByEmail(new Email("new@test.com"))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(new Email(creatorEmail))).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> provisionUserUseCase.execute(request, creatorEmail));
    }
}