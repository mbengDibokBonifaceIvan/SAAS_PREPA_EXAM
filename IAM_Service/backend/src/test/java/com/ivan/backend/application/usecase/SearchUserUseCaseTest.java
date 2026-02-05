package com.ivan.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.exception.DomainException;
import com.ivan.backend.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SearchUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SearchUserUseCase searchUserUseCase;

    @Test
    void should_throw_exception_when_accessing_user_from_different_tenant() {
        // Given
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        String ownerEmail = "owner@centerA.com";
        UUID targetId = UUID.randomUUID();

        User owner = mock(User.class);
        User target = mock(User.class);

        when(owner.getTenantId()).thenReturn(tenantA);
        when(target.getTenantId()).thenReturn(tenantB); // Tenant différent !
        
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(owner));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        // When & Then
        assertThrows(DomainException.class, () -> {
            searchUserUseCase.getUserById(targetId, ownerEmail);
        });
    }
}
