package com.ivan.notification_service.application;


import com.ivan.notification_service.application.usecase.OnboardingUseCaseImpl;
import com.ivan.notification_service.application.util.NotificationDispatcher;
import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OnboardingUseCaseTest {

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private OnboardingUseCaseImpl useCase;

    private final UUID userId = UUID.randomUUID();
    private final String email = "ivan@example.com";
    private final String name = "Ivan";

    @Nested
    @DisplayName("Bienvenue Organisation")
    class OrganizationWelcomeTests {
        @Test
        @DisplayName("Devrait envoyer un email de bienvenue avec le nom de l'organisation")
        void shouldHandleOrganizationWelcome() {
            String orgName = "Ivan Corp";
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

            useCase.handleOrganizationWelcome(userId, email, name, orgName);

            verify(dispatcher).dispatch(captor.capture());
            Notification notification = captor.getValue();

            assertEquals("🚀 Bienvenue à bord, Ivan Corp !", notification.getTitle());
            assertTrue(notification.getMessage().contains("Votre organisation 'Ivan Corp'"));
            assertEquals(NotificationType.EMAIL, notification.getType());
        }
    }

    @Nested
    @DisplayName("Activation de Compte")
    class AccountActivationTests {
        @Test
        @DisplayName("Devrait envoyer un email d'activation avec le contexte fluide")
        void shouldHandleAccountActivation() {
            String context = "Vérification d'identité réussie";
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

            useCase.handleAccountActivation(userId, email, name, context);

            verify(dispatcher).dispatch(captor.capture());
            Notification notification = captor.getValue();

            assertEquals("✅ Votre compte est opérationnel", notification.getTitle());
            assertTrue(notification.getMessage().contains("Motif : Vérification d'identité réussie"));
            assertTrue(notification.getMessage().contains("✨"));
        }
    }

    @Nested
    @DisplayName("Provisioning Utilisateur")
    class UserProvisionedTests {
        @Test
        @DisplayName("Devrait envoyer une invitation avec le rôle spécifié")
        void shouldHandleUserProvisioned() {
            String role = "STAFF_MEMBER";
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

            useCase.handleUserProvisioned(userId, email, name, role);

            verify(dispatcher).dispatch(captor.capture());
            Notification notification = captor.getValue();

            assertEquals("🎉 Invitation à rejoindre la plateforme", notification.getTitle());
            assertTrue(notification.getMessage().contains("Rôle attribué : STAFF_MEMBER"));
            assertEquals(email, notification.getRecipient());
        }
    }
}
