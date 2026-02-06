package com.ivan.backend.presentation.v1.rest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import; // Import ajouté
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.backend.application.dto.LogoutRequest;
import com.ivan.backend.application.port.in.LogoutInputPort;
import com.ivan.backend.infrastructure.adapter.identity.exception.KeycloakIdentityException;

import org.springframework.http.MediaType; // IMPORTANT: Spring MediaType, pas Jakarta

@WebMvcTest(LogoutController.class)
@ActiveProfiles("test")
// On importe manuellement la config Jackson si Spring ne la trouve pas
@Import(com.fasterxml.jackson.databind.ObjectMapper.class)
@AutoConfigureMockMvc(addFilters = false) // <--- DESACTIVE TOUS LES FILTRES DE SECURITE
class LogoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LogoutInputPort logoutInputPort;

    @Test
    @DisplayName("Logout : devrait retourner 204 quand le logout réussit")
    void shouldLogoutSuccessfully() throws Exception {
        // GIVEN
        LogoutRequest request = new LogoutRequest("valid-refresh-token");
        doNothing().when(logoutInputPort).execute(anyString());

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(logoutInputPort).execute("valid-refresh-token");
    }

    @Test
    @DisplayName("Logout : devrait retourner 400 si le refresh token est vide")
    void shouldReturn400WhenRefreshTokenIsBlank() throws Exception {
        // GIVEN
        LogoutRequest invalidRequest = new LogoutRequest("");

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(logoutInputPort);
    }

    @Test
    @DisplayName("Logout : devrait retourner 500 si Keycloak échoue (autre que identifiants)")
    void shouldReturn500WhenKeycloakFailsUnexpectedly() throws Exception {
        // GIVEN
        LogoutRequest request = new LogoutRequest("some-token");
        // On simule une erreur qui NE contient PAS "identifiants" pour tester la branche 500 de ton Handler
        doThrow(new KeycloakIdentityException("Erreur serveur Keycloak"))
                .when(logoutInputPort).execute(anyString());

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erreur Service Identité"));
    }
}
