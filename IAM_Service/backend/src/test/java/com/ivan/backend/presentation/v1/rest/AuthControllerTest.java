package com.ivan.backend.presentation.v1.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.backend.application.dto.LoginRequest;
import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.application.port.LoginInputPort;
import com.ivan.backend.domain.exception.KeycloakIdentityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class) // Importe ton gestionnaire d'erreurs ici
// Ajoute cette annotation pour ignorer la sécurité Spring dans ce test
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Supprime l' @Autowired ici
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private LoginInputPort loginUseCase;

    @Test
    @DisplayName("Devrait retourner 200 et le token si les identifiants sont corrects")
    void should_return_200_when_login_is_successful() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest("ivan@test.com", "password123");
        LoginResponse response = new LoginResponse(
            "access-jwt", "refresh-jwt", 3600, "ivan@test.com", false, "CENTER_OWNER"
        );
        
        when(loginUseCase.login(any(LoginRequest.class))).thenReturn(response);

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-jwt"))
            .andExpect(jsonPath("$.email").value("ivan@test.com"));
    }

    @Test
    @DisplayName("Devrait retourner 401 via le GlobalExceptionHandler si Keycloak échoue")
    void should_return_401_when_keycloak_fails() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest("bad@test.com", "wrong-pass");
        
        when(loginUseCase.login(any(LoginRequest.class)))
            .thenThrow(new KeycloakIdentityException("Identifiants invalides"));

        // WHEN & THEN
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Identifiants invalides"))
            .andExpect(jsonPath("$.status").value(401));
    }
}