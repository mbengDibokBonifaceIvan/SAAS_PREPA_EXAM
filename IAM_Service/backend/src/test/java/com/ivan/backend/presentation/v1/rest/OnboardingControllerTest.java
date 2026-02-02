package com.ivan.backend.presentation.v1.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;
import com.ivan.backend.application.usecase.OnboardingUseCase;
import com.ivan.backend.domain.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OnboardingController.class)
@AutoConfigureMockMvc(addFilters = false)
// On importe le handler d'exception pour que les erreurs 400 soient gérées
@Import({OnboardingController.class, GlobalExceptionHandler.class})
class OnboardingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OnboardingUseCase onboardingUseCase;

    @Configuration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void should_return_201_when_onboarding_is_valid() throws Exception {
        // Given
        OnboardingRequest request = new OnboardingRequest(
            "Ivan", "D", "ivan@test.com", "Pass123!", "Mon Centre");

        OnboardingResponse response = new OnboardingResponse(
            "Ivan", "D", false, UUID.randomUUID(), true, true);

        when(onboardingUseCase.execute(any(OnboardingRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.externalOrganizationId").exists());
    }

    @Test
    void should_return_400_when_user_already_exists() throws Exception {
        // Given
        OnboardingRequest request = new OnboardingRequest(
            "Ivan", "D", "duplicate@test.com", "Pass123!", "Mon Centre");

        when(onboardingUseCase.execute(any())).thenThrow(new UserAlreadyExistsException("duplicate@test.com"));

        // When & Then
        mockMvc.perform(post("/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void should_return_400_when_email_is_invalid() throws Exception {
        // Given (Email sans @)
        OnboardingRequest request = new OnboardingRequest(
            "Ivan", "D", "invalid-email", "Pass123!", "Mon Centre");

        // When & Then
        mockMvc.perform(post("/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}