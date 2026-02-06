package com.ivan.backend.presentation.v1.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.backend.application.dto.LoginRequest;
import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.application.port.in.LoginInputPort;
import com.ivan.backend.infrastructure.adapter.identity.exception.KeycloakIdentityException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class) // Importe ton gestionnaire d'erreurs ici
// Ajoute cette annotation pour ignorer la sécurité Spring dans ce test
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private LoginInputPort loginInputPort;

    @Test
    @DisplayName("Login : devrait retourner le token quand les identifiants sont corrects")
    void shouldLoginSuccessfully() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest("ivan@test.com", "password123");
        LoginResponse response = new LoginResponse("access-token-xyz", "refresh-token-abc", 3600L, "ivan@test.com",
                false, "UNIT_MANAGER");

        when(loginInputPort.login(any(LoginRequest.class))).thenReturn(response);

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-xyz"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.email").value("ivan@test.com"))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.role").value("UNIT_MANAGER"));

        verify(loginInputPort).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Login : devrait retourner 400 si l'email est vide")
    void shouldReturn400WhenRequestIsInvalid() throws Exception {
        // GIVEN : email vide et mot de passe vide
        LoginRequest invalidRequest = new LoginRequest("", "");

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // Grâce à @Valid et aux annotations dans le Record
    }

    @Test
    @DisplayName("Devrait retourner 401 via ProblemDetail si Keycloak échoue")
    void should_return_401_when_keycloak_fails() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest("bad@test.com", "wrong-pass");

        // Le message contient "identifiants" pour matcher ton Handler
        when(loginInputPort.login(any(LoginRequest.class)))
                .thenThrow(new KeycloakIdentityException("Identifiants invalides"));

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                // ProblemDetail utilise "detail" pour le message d'erreur
                .andExpect(jsonPath("$.detail").value("Identifiants invalides"))
                .andExpect(jsonPath("$.title").value("Erreur Service Identité"));
    }
}
