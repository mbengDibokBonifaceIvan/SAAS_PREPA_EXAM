package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.mapper.OnboardingMapper;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.event.OrganizationRegisteredEvent;
import com.ivan.backend.domain.port.IdentityManagerPort;
import com.ivan.backend.domain.port.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.service.OnboardingDomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingInteractorTest {

    @Mock private OnboardingDomainService onboardingDomainService;
    @Mock private IdentityManagerPort identityManagerPort;
    @Mock private MessagePublisherPort messagePublisherPort;
    @Mock private UserRepository userRepository;
    @Mock private OnboardingMapper mapper;

    @InjectMocks
    private OnboardingInteractor interactor;

    @Test
    void should_orchestrate_onboarding_process_correctly() {
        // GIVEN
        OnboardingRequest request = new OnboardingRequest("Ivan", "D", "ivan@test.com", "pass123", "Mon Centre");
        User mockUser = mock(User.class);
        OrganizationRegisteredEvent mockEvent = new OrganizationRegisteredEvent(
            UUID.randomUUID(), 
            "Mon Centre", 
            "ivan@test.com", 
            "Ivan", 
            "D" ,
            true
            
        );
        when(mapper.toDomain(any(), any())).thenReturn(mockUser);
        when(onboardingDomainService.initiateOnboarding(any(), anyString())).thenReturn(mockEvent);

        // WHEN
        interactor.execute(request);

        // THEN
        verify(userRepository).save(mockUser); // Vérifie que la DB locale est appelée
        verify(identityManagerPort).createIdentity(mockUser, "pass123"); // Vérifie l'appel Keycloak
        verify(messagePublisherPort).publishOrganizationRegistered(mockEvent); // Vérifie l'envoi RabbitMQ
        verify(mapper).toResponse(mockUser);
    }
}