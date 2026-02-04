package com.ivan.backend.presentation.v1.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ivan.backend.application.port.PasswordResetInputPort;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(PasswordController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordControllerTest {
    
    @Autowired 
    private MockMvc mockMvc;
    
    @MockitoBean
    private PasswordResetInputPort useCase;

    @Test
    void should_return_200_regardless_of_existence() throws Exception {
        String json = "{\"email\": \"any@test.com\"}";
        
        mockMvc.perform(post("/v1/auth/forgot-password")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON) // Utilise MediaType de Spring
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
