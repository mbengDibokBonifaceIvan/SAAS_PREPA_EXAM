package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;
import com.ivan.backend.application.mapper.OnboardingMapper;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.event.OrganizationRegisteredEvent;
import com.ivan.backend.domain.exception.UserAlreadyExistsException;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.port.out.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.service.OnboardingDomainService;
import com.ivan.backend.domain.valueobject.Email;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OnboardingUseCaseTest {

    @Mock
    private OnboardingDomainService onboardingDomainService;
    @Mock
    private IdentityManagerPort identityManagerPort;
    @Mock
    private MessagePublisherPort messagePublisherPort;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OnboardingMapper mapper;

    @InjectMocks
    private OnboardingUseCase onboardingUseCase;

    @Test
    @DisplayName("Onboarding : succès nominal")
    void shouldOnboardSuccessfully() {
        // GIVEN
        OnboardingRequest request = new OnboardingRequest("Ivan", "Lastname", "ivan@test.com", "Pass123!", "DevCorp");

        User user = mock(User.class);
        OrganizationRegisteredEvent event = mock(OrganizationRegisteredEvent.class);

        // FIX: Configurer le mock user pour éviter le NPE lors du log.info
        when(user.getEmail()).thenReturn(new Email("ivan@test.com"));

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(mapper.toDomain(any(), any(UUID.class))).thenReturn(user);
        when(onboardingDomainService.initiateOnboarding(any(), anyString())).thenReturn(event);

        when(mapper.toResponse(user)).thenReturn(new OnboardingResponse(
                "Ivan", "Lastname", true, UUID.randomUUID(), false, false));

        // WHEN
        OnboardingResponse response = onboardingUseCase.execute(request);

        // THEN
        assertNotNull(response);
        verify(userRepository).save(user);
        verify(identityManagerPort).createIdentity(user, "Pass123!");
        verify(messagePublisherPort).publishOrganizationRegistered(event);
    }

    @Test
    @DisplayName("Onboarding : devrait échouer si l'utilisateur existe déjà")
    void shouldThrowExceptionWhenUserExists() {
        // GIVEN
        OnboardingRequest request = new OnboardingRequest(
                "Ivan", // firstName
                "Lastname", // lastName
                "ivan@test.com", // email
                "Pass123!", // password
                "DevCorp" // organizationName
        );
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(mock(User.class)));

        // WHEN & THEN
        assertThrows(UserAlreadyExistsException.class, () -> onboardingUseCase.execute(request));

        // Vérifie qu'on s'est arrêté tout de suite (Fail-fast)
        verifyNoInteractions(identityManagerPort, messagePublisherPort, onboardingDomainService);
    }

    @Test
    @DisplayName("Onboarding : devrait propager l'erreur si l'IAM échoue")
    void shouldRollbackWhenIamFails() {
        // GIVEN
        OnboardingRequest request = new OnboardingRequest(
                "Ivan", // firstName
                "Lastname", // lastName
                "ivan@test.com", // email
                "Pass123!", // password
                "DevCorp" // organizationName
        );
        User user = mock(User.class);

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(mapper.toDomain(any(), any(UUID.class))).thenReturn(user);

        // Simuler un crash de Keycloak
        doThrow(new RuntimeException("Keycloak Offline"))
                .when(identityManagerPort).createIdentity(any(), anyString());

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> onboardingUseCase.execute(request));

        // On vérifie que le message n'est JAMAIS publié si Keycloak a échoué
        verifyNoInteractions(messagePublisherPort);
    }
}