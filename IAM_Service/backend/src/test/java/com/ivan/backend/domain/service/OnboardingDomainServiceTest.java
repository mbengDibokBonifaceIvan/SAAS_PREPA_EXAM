package com.ivan.backend.domain.service;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.exception.UserAlreadyExistsException;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OnboardingDomainServiceTest {

    private UserRepository userRepository;
    private OnboardingDomainService onboardingService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        onboardingService = new OnboardingDomainService(userRepository);
    }

    @Test
    void should_initiate_onboarding_successfully() {
        // Given
        User owner = new User(null, "Ivan", "Test", new Email("ivan@test.com"), UUID.randomUUID(), null, UserRole.CENTER_OWNER, true, true, false);
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // When
        var event = onboardingService.initiateOnboarding(owner, "Mon Centre");

        // Then
        assertNotNull(event);
        assertEquals("Mon Centre", event.organizationName());
        assertEquals("ivan@test.com", event.ownerEmail());
    }

    @Test
    void should_throw_exception_when_user_email_already_exists() {
        // Given
        User owner = new User(null, "Ivan", "Test", new Email("exist@test.com"), UUID.randomUUID(), null, UserRole.CENTER_OWNER, true, false,true);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(owner));

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> {
            onboardingService.initiateOnboarding(owner, "Autre Centre");
        });
    }
}