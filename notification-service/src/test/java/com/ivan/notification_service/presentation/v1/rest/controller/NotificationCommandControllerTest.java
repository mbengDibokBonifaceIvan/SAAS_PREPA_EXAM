package com.ivan.notification_service.presentation.v1.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.notification_service.application.dto.FeedbackRequest;
import com.ivan.notification_service.application.dto.SecurityAlertRequest;
import com.ivan.notification_service.application.dto.UserNotificationRequest;
import com.ivan.notification_service.application.port.in.OnboardingUseCase;
import com.ivan.notification_service.application.port.in.ProcessGenericFeedbackUseCase;
import com.ivan.notification_service.application.port.in.SendSecurityNotificationUseCase;
import com.ivan.notification_service.infrastructure.adapter.in.push.ToastPushNotificationAdapter;
import com.ivan.notification_service.presentation.v1.rest.controller.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationCommandController.class)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class NotificationCommandControllerTest {

        @Autowired
        private MockMvc mockMvc;

        private ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private SendSecurityNotificationUseCase securityUseCase;

        @MockitoBean
        private OnboardingUseCase onboardingUseCase;

        @MockitoBean
        private ProcessGenericFeedbackUseCase feedbackUseCase;

        @MockitoBean
        private ToastPushNotificationAdapter sseAdapter;

        @Test
        @DisplayName("Security Alert : devrait accepter le JSON et appeler le use case")
        void shouldHandleSecurityAlert() throws Exception {
                UUID userId = UUID.randomUUID();
                SecurityAlertRequest request = new SecurityAlertRequest(
                                userId, "test@test.com", "Ivan", "USER_LOCKED", "Tentatives infructueuses");

                mockMvc.perform(post("/api/v1/notifications/commands/security-alert")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isAccepted());

                verify(securityUseCase).handle(eq(userId), eq("test@test.com"), eq("Ivan"), eq("USER_LOCKED"),
                                eq("Tentatives infructueuses"));
        }

        @Test
        @DisplayName("Welcome Org : devrait mapper le corps JSON vers le onboarding use case")
        void shouldHandleWelcomeOrg() throws Exception {
                UUID userId = UUID.randomUUID();
                UserNotificationRequest request = new UserNotificationRequest(
                                userId, "boss@corp.com", "Le Boss", "Skilyo");

                mockMvc.perform(post("/api/v1/notifications/commands/welcome-org")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isAccepted());

                verify(onboardingUseCase).handleOrganizationWelcome(eq(userId), eq("boss@corp.com"), eq("Le Boss"),
                                eq("Skilyo"));
        }

        @Test
        @DisplayName("Feedback : devrait accepter un corps JSON et retourner 202")
        void shouldHandleFeedbackJson() throws Exception {
                FeedbackRequest request = new FeedbackRequest(
                                UUID.randomUUID(), "Titre Test", "Message Test", "INFO");

                mockMvc.perform(post("/api/v1/notifications/commands/feedback")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isAccepted());

                verify(feedbackUseCase).handle(any(FeedbackRequest.class));
        }

        @Test
        @DisplayName("SSE Subscribe : devrait appeler l'adapter SSE")
        void shouldSubscribeToSse() throws Exception {
                UUID userId = UUID.randomUUID();

                mockMvc.perform(get("/api/v1/notifications/commands/{userId}", userId)
                                .accept(MediaType.TEXT_EVENT_STREAM))
                                .andExpect(status().isOk());

                verify(sseAdapter).registerClient(userId);
        }

        @Test
        @DisplayName("Validation : devrait retourner 400 si le JSON est invalide (email incorrect)")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
                // GIVEN: Email invalide
                UserNotificationRequest request = new UserNotificationRequest(
                                UUID.randomUUID(), "pas-un-email", "Ivan", "Dev");

                // WHEN & THEN
                mockMvc.perform(post("/api/v1/notifications/commands/account-activation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.title").value("Erreur de validation"))
                                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("email")));
        }

        @Test
        @DisplayName("Password Reset : devrait appeler securityUseCase avec le message de reset")
        void shouldHandlePasswordReset() throws Exception {
                UUID userId = UUID.randomUUID();
                UserNotificationRequest request = new UserNotificationRequest(userId, "user@test.com", "Ivan", null);

                mockMvc.perform(post("/api/v1/notifications/commands/password-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isAccepted());

                verify(securityUseCase).handle(eq(userId), eq("user@test.com"), eq("Ivan"),
                                eq("RÉINITIALISATION DE MOT DE PASSE"), eq("Demande de modification reçue."));
        }

        @Test
        @DisplayName("Account Activation : devrait appeler onboardingUseCase avec la raison")
        void shouldHandleAccountActivation() throws Exception {
                UUID userId = UUID.randomUUID();
                UserNotificationRequest request = new UserNotificationRequest(userId, "user@test.com", "Ivan",
                                "Compte validé");

                mockMvc.perform(post("/api/v1/notifications/commands/account-activation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isAccepted());

                verify(onboardingUseCase).handleAccountActivation(eq(userId), eq("user@test.com"), eq("Ivan"),
                                eq("Compte validé"));
        }

        @Test
        @DisplayName("User Provisioned : devrait appeler onboardingUseCase avec le rôle")
        void shouldHandleUserProvisioned() throws Exception {
                UUID userId = UUID.randomUUID();
                UserNotificationRequest request = new UserNotificationRequest(userId, "staff@test.com", "Staff User",
                                "ROLE_ADMIN");

                mockMvc.perform(post("/api/v1/notifications/commands/user-provisioned")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isAccepted());

                verify(onboardingUseCase).handleUserProvisioned(eq(userId), eq("staff@test.com"), eq("Staff User"),
                                eq("ROLE_ADMIN"));
        }
}