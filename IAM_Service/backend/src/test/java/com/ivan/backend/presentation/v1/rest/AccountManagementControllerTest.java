package com.ivan.backend.presentation.v1.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.backend.application.dto.ProvisionUserRequest;
import com.ivan.backend.application.port.ProvisionUserInputPort;
import com.ivan.backend.application.usecase.SearchUserUseCase;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    @MockitoBean
    private SearchUserUseCase searchUserUseCase;

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

    @Test
    void should_return_directory_for_owner() throws Exception {
        // Given
        String ownerEmail = "owner@test.com";
        User mockUser = new User(UUID.randomUUID(), "John", "Doe", new Email("john@doe.com"), 
                                UUID.randomUUID(), null, UserRole.STAFF_MEMBER, true, true, false);
        
        when(searchUserUseCase.getDirectory(eq(ownerEmail), any())).thenReturn(List.of(mockUser));

        // When & Then
        mockMvc.perform(get("/v1/accounts/directory")
                        .with(jwt().jwt(j -> j.claim("email", ownerEmail))
                                   .authorities(new SimpleGrantedAuthority("ROLE_CENTER_OWNER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].email").value("john@doe.com"));
    }

    @Test
    void should_return_my_profile() throws Exception {
        String email = "me@test.com";
        User mockUser = new User(UUID.randomUUID(), "Ivan", "M", new Email(email), 
                                UUID.randomUUID(), null, UserRole.CENTER_OWNER, true, true, false);

        when(searchUserUseCase.getUserProfile(email)).thenReturn(mockUser);

        mockMvc.perform(get("/v1/accounts/me")
                        .with(jwt().jwt(j -> j.claim("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CENTER_OWNER"));
    }

    @Test
    void should_return_user_by_id_when_authorized() throws Exception {
        UUID targetId = UUID.randomUUID();
        String ownerEmail = "owner@test.com";
        User mockUser = new User(targetId, "Target", "User", new Email("target@test.com"), 
                                UUID.randomUUID(), null, UserRole.STAFF_MEMBER, true, true, false);

        when(searchUserUseCase.getUserById(targetId, eq(ownerEmail))).thenReturn(mockUser);

        mockMvc.perform(get("/v1/accounts/" + targetId)
                        .with(jwt().jwt(j -> j.claim("email", ownerEmail))
                                   .authorities(new SimpleGrantedAuthority("ROLE_CENTER_OWNER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId.toString()));
    }
    
    
}