package com.ivan.notification_service.presentation.v1.rest.controller;

import com.ivan.notification_service.application.dto.FeedbackRequest;
import com.ivan.notification_service.application.port.in.OnboardingUseCase;
import com.ivan.notification_service.application.port.in.ProcessGenericFeedbackUseCase;
import com.ivan.notification_service.application.port.in.SendSecurityNotificationUseCase;
import com.ivan.notification_service.infrastructure.adapter.in.push.ToastPushNotificationAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationCommandController.class)
@ActiveProfiles("test")
class NotificationCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SendSecurityNotificationUseCase securityUseCase;

    @MockitoBean
    private OnboardingUseCase onboardingUseCase;

    @MockitoBean
    private ProcessGenericFeedbackUseCase feedbackUseCase;

    @MockitoBean
    private ToastPushNotificationAdapter sseAdapter;

    @Test
    @DisplayName("Security Alert : devrait appeler le use case et retourner 202")
    void shouldHandleSecurityAlert() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/notifications/commands/security-alert")
                .param("userId", userId.toString())
                .param("email", "test@test.com")
                .param("name", "Ivan")
                .param("alertType", "USER_LOCKED")
                .param("reason", "Tentatives infructueuses"))
                .andExpect(status().isAccepted());

        verify(securityUseCase).handle(userId, "test@test.com", "Ivan", "USER_LOCKED", "Tentatives infructueuses");
    }

    @Test
    @DisplayName("Welcome Org : devrait mapper correctement les paramètres d'organisation")
    void shouldHandleWelcomeOrg() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/notifications/commands/welcome-org")
                .param("userId", userId.toString())
                .param("email", "boss@corp.com")
                .param("name", "Le Boss")
                .param("orgName", "Skilyo"))
                .andExpect(status().isAccepted());

        verify(onboardingUseCase).handleOrganizationWelcome(userId, "boss@corp.com", "Le Boss", "Skilyo");
    }

    @Test
    @DisplayName("Feedback : devrait accepter un corps JSON et retourner 202")
    void shouldHandleFeedbackJson() throws Exception {
        // GIVEN
        FeedbackRequest request = new FeedbackRequest(
                UUID.randomUUID(),
                "Titre Test",
                "Message Test",
                "INFO");
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request);

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/notifications/commands/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isAccepted());

        verify(feedbackUseCase).handle(any(FeedbackRequest.class));
    }

    @Test
    @DisplayName("SSE Subscribe : devrait appeler l'adapteur d'enregistrement")
    void shouldSubscribeToSse() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/notifications/commands/" + userId)
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());

        verify(sseAdapter).registerClient(userId);
    }

    @Test
    @DisplayName("Validation : devrait retourner 400 si un paramètre obligatoire manque")
    void shouldReturn400WhenParamMissing() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/commands/security-alert")
                // Il manque userId et les autres
                .param("email", "test@test.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Paramètre manquant"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("userId")));
    }
}
