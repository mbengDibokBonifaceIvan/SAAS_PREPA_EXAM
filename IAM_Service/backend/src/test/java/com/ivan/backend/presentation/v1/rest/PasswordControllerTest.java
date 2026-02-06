package com.ivan.backend.presentation.v1.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.backend.application.dto.ForgotPasswordRequest;
import com.ivan.backend.application.port.in.PasswordResetInputPort;
import com.ivan.backend.infrastructure.adapter.identity.exception.KeycloakIdentityException;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(PasswordController.class)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PasswordResetInputPort passwordResetInputPort;

    @Test
    @DisplayName("Forgot Password : devrait toujours retourner 200 pour éviter l'énumération")
    void shouldRequestPasswordResetSuccessfully() throws Exception {
        // GIVEN
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@test.com");
        doNothing().when(passwordResetInputPort).requestReset(anyString());

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("réinitialisation a été envoyée")));

        verify(passwordResetInputPort).requestReset("user@test.com");
    }

    @Test
    @DisplayName("Forgot Password : devrait retourner 500 si le service d'identité tombe")
    void shouldReturn500WhenIdentityServiceFails() throws Exception {
        // GIVEN
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@test.com");
        
        // Simule une erreur technique (pas une erreur d'identifiants)
        doThrow(new KeycloakIdentityException("SMTP Error"))
                .when(passwordResetInputPort).requestReset(anyString());

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erreur Service Identité"));
    }
}