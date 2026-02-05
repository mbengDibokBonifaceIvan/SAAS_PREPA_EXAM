package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.ProvisionUserRequest;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.exception.DomainException;
import com.ivan.backend.domain.port.IdentityManagerPort;
import com.ivan.backend.domain.port.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvisionUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private IdentityManagerPort identityManagerPort;
    @Mock private MessagePublisherPort messagePublisher;

    @InjectMocks private ProvisionUserUseCase useCase;

    private User owner;
    private UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        owner = new User(UUID.randomUUID(), "Owner", "Boss", new Email("owner@test.com"),
                tenantId, null, UserRole.CENTER_OWNER, true, true, false);
    }

    @Test
    void should_provision_user_successfully() {
        // Given
        ProvisionUserRequest request = new ProvisionUserRequest(
                "John", "Doe", "john@test.com", UserRole.STAFF_MEMBER, UUID.randomUUID());
        
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(owner));

        // When
        useCase.execute(request, "owner@test.com");

        // Then
        verify(identityManagerPort).createIdentity(any(), anyString());
        verify(userRepository).save(any());
        verify(messagePublisher).publishUserProvisioned(any());
    }

    @Test
    void should_fail_when_creator_has_lower_role() {
        // Given: Un Staff essaie de créer un Owner (Interdit)
        User staff = new User(UUID.randomUUID(), "Staff", "S", new Email("staff@test.com"),
                tenantId, null, UserRole.STAFF_MEMBER, true, true, false);
        
        ProvisionUserRequest request = new ProvisionUserRequest(
                "Boss", "B", "boss@test.com", UserRole.CENTER_OWNER, null);
        
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(staff));

        // When & Then
        assertThrows(DomainException.class, () -> useCase.execute(request, "staff@test.com"));
        verifyNoInteractions(identityManagerPort);
    }
}