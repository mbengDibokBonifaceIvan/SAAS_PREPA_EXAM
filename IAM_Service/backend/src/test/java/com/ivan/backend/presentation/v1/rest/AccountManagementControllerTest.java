package com.ivan.backend.presentation.v1.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.backend.application.dto.ProvisionUserRequest;
import com.ivan.backend.application.port.in.ManageAccountInputPort;
import com.ivan.backend.application.port.in.ProvisionUserInputPort;
import com.ivan.backend.application.port.in.SearchUserInputPort;
import com.ivan.backend.application.port.in.UpdateUserInputPort;
import com.ivan.backend.domain.valueobject.UserRole;
import com.ivan.backend.infrastructure.config.SecurityConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountManagementController.class)
@ActiveProfiles("test")
@Import({ SecurityConfig.class, GlobalExceptionHandler.class }) // <-- Ajoute ton Handler ici
class AccountManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProvisionUserInputPort provisionUserUseCase;
    @MockitoBean
    private ManageAccountInputPort manageAccountInputPort;
    @MockitoBean
    private SearchUserInputPort searchUserPort;
    @MockitoBean
    private UpdateUserInputPort updateUserInputPort;

    @Test
    @DisplayName("Provision : devrait retourner 201 avec les droits appropriés")
    void shouldProvisionAccountSuccessfully() throws Exception {
        // GIVEN
        ProvisionUserRequest request = new ProvisionUserRequest(
                "Ivan", "Test", "new@test.com", UserRole.CANDIDATE, UUID.randomUUID());

        // WHEN & THEN
        mockMvc.perform(post("/v1/accounts/provision")
                // Correction ici : on utilise .jwt(builder -> builder.claim(...))
                .with(jwt().jwt(builder -> builder.claim("email", "admin@test.com"))
                        .authorities(new SimpleGrantedAuthority("ROLE_CENTER_OWNER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        verify(provisionUserUseCase).execute(any(), eq("admin@test.com"));
    }

    @Test
    @DisplayName("Ban : devrait retourner 204 et appeler le Use Case")
    void shouldBanAccount() throws Exception {
        // WHEN & THEN
        mockMvc.perform(patch("/v1/accounts/target@test.com/ban")
                .with(jwt().jwt(builder -> builder.claim("email", "owner@test.com"))
                        .authorities(new SimpleGrantedAuthority("ROLE_CENTER_OWNER"))))
                .andExpect(status().isNoContent());

        verify(manageAccountInputPort).banAccount("target@test.com", "owner@test.com");
    }

    @Test
    @DisplayName("Security : devrait retourner 403 si un CANDIDATE essaie d'accéder à l'annuaire")
    void shouldReturn403WhenCandidateAccessDirectory() throws Exception {
        mockMvc.perform(get("/v1/accounts/directory")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                .andExpect(status().isForbidden()); // Maintenant, @PreAuthorize va bloquer !
    }

    @Test
    @DisplayName("Directory : devrait passer le unitId optionnel s'il est présent")
    void shouldFetchDirectoryWithUnitId() throws Exception {
        UUID unitId = UUID.randomUUID();

        mockMvc.perform(get("/v1/accounts/directory")
                .param("unitId", unitId.toString())
                .with(jwt().jwt(builder -> builder.claim("email", "manager@test.com"))
                        .authorities(new SimpleGrantedAuthority("ROLE_UNIT_MANAGER"))))
                .andExpect(status().isOk());

        verify(searchUserPort).getDirectory("manager@test.com", unitId);
    }
}