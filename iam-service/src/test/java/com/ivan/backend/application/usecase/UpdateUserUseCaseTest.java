package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.UpdateUserRequest;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.exception.InsufficientPrivilegesException;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private IdentityManagerPort identityManagerPort;

    @InjectMocks
    private UpdateUserUseCase updateUserUseCase;

    @Test
    @DisplayName("Update : succès de la mise à jour du profil (Nom/Prénom)")
    void shouldUpdateProfileSuccessfully() {
        // GIVEN
        UUID targetId = UUID.randomUUID();
        String requesterEmail = "manager@test.com";
        UpdateUserRequest request = new UpdateUserRequest("NewName", "NewLast", null, null);

        User requester = mock(User.class);
        User target = mock(User.class);
        when(target.getEmail()).thenReturn(new Email("target@test.com"));

        when(userRepository.findByEmail(new Email(requesterEmail))).thenReturn(Optional.of(requester));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        // WHEN
        updateUserUseCase.execute(targetId, requesterEmail, request);

        // THEN
        verify(requester).checkCanManage(target);
        verify(target).updateProfile("NewName", "NewLast");
        verify(userRepository).save(target);
        // On vérifie qu'on n'a pas touché à l'admin (Keycloak)
        verifyNoInteractions(identityManagerPort);
    }

    @Test
    @DisplayName("Update Admin : succès du changement de rôle par un Owner")
    void shouldUpdateRoleSuccessfully_WhenRequesterIsOwner() {
        // GIVEN
        UUID targetId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        String ownerEmail = "owner@test.com";
        UpdateUserRequest request = new UpdateUserRequest(null, null, UserRole.UNIT_MANAGER, null);

        User owner = mock(User.class);
        User target = mock(User.class);
        when(owner.getRole()).thenReturn(UserRole.CENTER_OWNER);
        when(owner.getId()).thenReturn(requesterId);
        when(target.getEmail()).thenReturn(new Email("target@test.com"));

        when(userRepository.findByEmail(new Email(ownerEmail))).thenReturn(Optional.of(owner));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        // WHEN
        updateUserUseCase.execute(targetId, ownerEmail, request);

        // THEN
        verify(target).changeRole(UserRole.UNIT_MANAGER, requesterId);
        verify(identityManagerPort).updateUserRole("target@test.com", "UNIT_MANAGER");
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("Update Admin : devrait échouer si un non-owner tente de changer un rôle")
    void shouldThrowException_WhenNonOwnerUpdatesAdminFields() {
        // GIVEN
        UUID targetId = UUID.randomUUID();
        String managerEmail = "manager@test.com";
        UpdateUserRequest request = new UpdateUserRequest(null, null, UserRole.CENTER_OWNER, null);

        User manager = mock(User.class);
        User target = mock(User.class);
        when(manager.getRole()).thenReturn(UserRole.UNIT_MANAGER); // Pas Owner

        when(userRepository.findByEmail(new Email(managerEmail))).thenReturn(Optional.of(manager));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        // WHEN & THEN
        assertThrows(InsufficientPrivilegesException.class, () -> 
            updateUserUseCase.execute(targetId, managerEmail, request)
        );
        
        verify(userRepository, never()).save(any());
    }
}