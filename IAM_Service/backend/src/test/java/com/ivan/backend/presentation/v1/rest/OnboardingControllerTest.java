package com.ivan.backend.presentation.v1.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;
import com.ivan.backend.application.port.in.OnboardingInputPort;
import com.ivan.backend.domain.exception.UserAlreadyExistsException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OnboardingController.class)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OnboardingInputPort onboardingInputPort;

    @Test
    @DisplayName("Onboarding : devrait retourner 201")
    void shouldRegisterOrganizationSuccessfully() throws Exception {
        // RESPECTE L'ORDRE : firstName, lastName, email, password, organizationName
        OnboardingRequest request = new OnboardingRequest(
                "Ivan",
                "Dev",
                "admin@entreprise.com",
                "Password123!",
                "Ma Super Entreprise");

        UUID orgId = UUID.randomUUID();
        OnboardingResponse response = new OnboardingResponse(
                "Ma Super Entreprise",
                "admin@entreprise.com",
                true,
                orgId,
                false,
                true);
        when(onboardingInputPort.execute(any(OnboardingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrganizationId").value(orgId.toString()));
    }

    @Test
    @DisplayName("Onboarding : devrait retourner 409 quand l'utilisateur existe déjà")
    void should_return_409_when_user_already_exists() throws Exception {
        // ORDRE CORRECT ICI AUSSI
        OnboardingRequest request = new OnboardingRequest(
                "Ivan", "D", "duplicate@test.com", "Pass123!", "Mon Centre");

        when(onboardingInputPort.execute(any())).thenThrow(new UserAlreadyExistsException("L'utilisateur existe déjà"));

        mockMvc.perform(post("/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()) // 409 selon ton Handler
                .andExpect(jsonPath("$.title").value("Conflit d'identité"));
    }

    @Test
    @DisplayName("Onboarding : devrait retourner 400 quand le corps de la requête est invalide")
    void shouldReturn400WhenRequestIsMalformed() throws Exception {
        // GIVEN : Un objet vide ou incomplet qui devrait échouer à la validation @Valid
        OnboardingRequest invalidRequest = new OnboardingRequest("", "", "", "", "");

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Onboarding : devrait retourner 400 quand l'email est invalide")
    void should_return_400_when_email_is_invalid() throws Exception {
        // Email mal formé
        OnboardingRequest request = new OnboardingRequest(
                "Ivan", "invalid-email", "Pass123!", "Ivan", "Dev");

        mockMvc.perform(post("/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation échouée"));
    }
}