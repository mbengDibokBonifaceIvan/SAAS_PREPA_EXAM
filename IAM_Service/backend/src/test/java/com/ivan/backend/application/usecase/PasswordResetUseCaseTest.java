package com.ivan.backend.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.port.IdentityManagerPort;
import com.ivan.backend.domain.port.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;

@ExtendWith(MockitoExtension.class)
class PasswordResetUseCaseTest {
    @Mock private UserRepository userRepository;
    @Mock private IdentityManagerPort identityManagerPort;
    @Mock private MessagePublisherPort messagePublisher;
    @InjectMocks private PasswordResetUseCase useCase;

  @Test
    void should_call_identity_manager_only_if_user_active() {
        // GIVEN
        String emailStr = "test@ivan.com";
        Email emailVo = new Email(emailStr);
        
        // Utilise ton constructeur habituel ici
        User activeUser = new User(null, emailStr, emailStr, emailVo, null, null, null, false, true, false); 

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(activeUser));

        // WHEN
        useCase.requestReset(emailStr);

        // THEN
        verify(identityManagerPort).sendPasswordReset(emailStr);
        verify(messagePublisher).publishPasswordResetRequested(any());
    }

    @Test
    void should_not_trigger_anything_if_user_not_found() {
        // GIVEN
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // WHEN
        useCase.requestReset("ghost@test.com");

        // THEN
        verifyNoInteractions(identityManagerPort);
    }
}