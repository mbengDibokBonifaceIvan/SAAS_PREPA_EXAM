package com.ivan.backend.presentation.v1.rest;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import; // Import ajouté
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.backend.application.dto.LogoutRequest;
import com.ivan.backend.application.usecase.LogoutUseCase;
import org.springframework.http.MediaType; // IMPORTANT: Spring MediaType, pas Jakarta

@WebMvcTest(LogoutController.class)
// On importe manuellement la config Jackson si Spring ne la trouve pas
@Import(com.fasterxml.jackson.databind.ObjectMapper.class)
@AutoConfigureMockMvc(addFilters = false) // <--- DESACTIVE TOUS LES FILTRES DE SECURITE
class LogoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogoutUseCase logoutUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturn204WhenLogoutIsSuccessful() throws Exception {
        LogoutRequest request = new LogoutRequest("some-refresh-token");

        mockMvc.perform(post("/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(logoutUseCase).execute("some-refresh-token");
    }
}
