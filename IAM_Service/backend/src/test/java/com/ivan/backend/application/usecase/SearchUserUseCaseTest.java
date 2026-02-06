package com.ivan.backend.application.usecase;

import com.ivan.backend.domain.entity.User;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SearchUserUseCaseTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private SearchUserUseCase searchUserUseCase;

    @Test
    @DisplayName("Directory : CENTER_OWNER sans filtre devrait voir tout le tenant")
    void shouldReturnFullDirectory_ForCenterOwner() {
        // GIVEN
        String email = "owner@test.com";
        UUID tenantId = UUID.randomUUID();
        User owner = mock(User.class);
        
        when(owner.getRole()).thenReturn(UserRole.CENTER_OWNER);
        when(owner.getTenantId()).thenReturn(tenantId);
        when(userRepository.findByEmail(new Email(email))).thenReturn(Optional.of(owner));
        when(userRepository.findAllByTenantId(tenantId)).thenReturn(List.of(owner, mock(User.class)));

        // WHEN
        List<User> result = searchUserUseCase.getDirectory(email, null);

        // THEN
        assertEquals(2, result.size());
        verify(userRepository).findAllByTenantId(tenantId);
    }

    @Test
    @DisplayName("Directory : UNIT_MANAGER devrait être restreint à son unité")
    void shouldReturnUnitDirectory_ForUnitManager() {
        // GIVEN
        String email = "manager@test.com";
        UUID unitId = UUID.randomUUID();
        User manager = mock(User.class);
        
        when(manager.getRole()).thenReturn(UserRole.UNIT_MANAGER);
        when(manager.getUnitId()).thenReturn(unitId);
        when(userRepository.findByEmail(new Email(email))).thenReturn(Optional.of(manager));
        when(userRepository.findAllByUnitId(unitId)).thenReturn(List.of(manager));

        // WHEN
        List<User> result = searchUserUseCase.getDirectory(email, null);

        // THEN
        assertEquals(1, result.size());
        verify(userRepository).findAllByUnitId(unitId);
    }

    @Test
    @DisplayName("Directory : Un candidat ne devrait voir que lui-même")
    void shouldReturnOnlySelf_ForCandidate() {
        // GIVEN
        String email = "candidate@test.com";
        User candidate = mock(User.class);
        
        when(candidate.getRole()).thenReturn(UserRole.CANDIDATE);
        when(userRepository.findByEmail(new Email(email))).thenReturn(Optional.of(candidate));

        // WHEN
        List<User> result = searchUserUseCase.getDirectory(email, null);

        // THEN
        assertEquals(1, result.size());
        assertEquals(candidate, result.get(0));
        verify(userRepository, never()).findAllByTenantId(any());
    }

    @Test
    @DisplayName("getUserById : devrait valider les droits via l'entité")
    void shouldGetUserById_WithSecurityCheck() {
        // GIVEN
        UUID targetId = UUID.randomUUID();
        String requesterEmail = "admin@test.com";
        User requester = mock(User.class);
        User target = mock(User.class);

        when(userRepository.findByEmail(new Email(requesterEmail))).thenReturn(Optional.of(requester));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        // WHEN
        User result = searchUserUseCase.getUserById(targetId, requesterEmail);

        // THEN
        assertNotNull(result);
        verify(requester).checkCanManage(target); // La validation métier est appelée
    }

    @Test
    @DisplayName("getUserById : devrait lever une exception si l'utilisateur n'existe pas")
    void shouldThrowException_WhenUserNotFound() {
        // GIVEN
        UUID unknownId = UUID.randomUUID();
        String requesterEmail = "admin@test.com";
        when(userRepository.findByEmail(new Email(requesterEmail))).thenReturn(Optional.of(mock(User.class)));
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> searchUserUseCase.getUserById(unknownId, requesterEmail));
    }
}