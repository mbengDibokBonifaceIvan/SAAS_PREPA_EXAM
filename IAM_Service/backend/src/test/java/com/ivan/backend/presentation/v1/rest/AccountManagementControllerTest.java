package com.ivan.backend.presentation.v1.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.backend.application.dto.ProvisionUserRequest;
import com.ivan.backend.application.port.ProvisionUserInputPort;
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest(AccountManagementController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureJsonTesters
class AccountManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // On l'instancie manuellement pour éviter les caprices du contexte de test
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProvisionUserInputPort provisionUserUseCase;

    @Test
    void should_return_201_when_provisioning_is_successful() throws Exception {
        // Given
        ProvisionUserRequest request = new ProvisionUserRequest(
                "Staff", "Member", "staff@exams.com", UserRole.STAFF_MEMBER, UUID.randomUUID());

        // When & Then
        mockMvc.perform(post("/v1/accounts/provision")
                .with(jwt()
                        // 1. On ajoute l'autorité pour passer le @PreAuthorize
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_CENTER_OWNER")) // 2. On ajoute l'email pour le Use Case
                        .jwt(j -> j.claim("email", "owner@test.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("Utilisateur provisionné avec succès. Un email d'activation lui a été envoyé."));

        verify(provisionUserUseCase).execute(any(ProvisionUserRequest.class), eq("owner@test.com"));
    }

    @Test
    void should_return_403_when_user_is_not_authenticated() throws Exception {
        mockMvc.perform(post("/v1/accounts/provision")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }
}