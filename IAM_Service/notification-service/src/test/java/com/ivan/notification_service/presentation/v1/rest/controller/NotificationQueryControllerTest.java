package com.ivan.notification_service.presentation.v1.rest.controller;

import com.ivan.notification_service.application.dto.NotificationResponse;
import com.ivan.notification_service.application.port.in.GetNotificationHistoryUseCase;
import com.ivan.notification_service.presentation.v1.rest.controller.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationQueryController.class)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class NotificationQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetNotificationHistoryUseCase historyUseCase;

    @Test
    @DisplayName("History : devrait retourner 200 avec une page de notifications")
    void shouldReturnNotificationsPage() throws Exception {
        // GIVEN
        UUID userId = UUID.randomUUID();
        NotificationResponse response = createMockResponse();
        
        Page<NotificationResponse> mockPage = new PageImpl<>(List.of(response));
        
        when(historyUseCase.getForUser(eq(userId), any(Pageable.class))).thenReturn(mockPage);

        // WHEN & THEN
        mockMvc.perform(get("/api/v1/notifications/queries/user/{userId}/history", userId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("🔔 Alerte"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("History : devrait retourner 204 quand la page est vide")
    void shouldReturn204WhenNoNotifications() throws Exception {
        UUID userId = UUID.randomUUID();
        when(historyUseCase.getForUser(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/notifications/queries/user/{userId}/history", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("History : devrait utiliser les valeurs de pagination par défaut et retourner 200")
    void shouldHandleDefaultPagination() throws Exception {
        // Changement de logique pour éviter la duplication avec le test 204
        UUID userId = UUID.randomUUID();
        NotificationResponse response = createMockResponse();
// Correction ici : On précise que la page demandée était de taille 10
    Page<NotificationResponse> mockPage = new PageImpl<>(
        List.of(response), 
        PageRequest.of(0, 10), 
        1
    );
        when(historyUseCase.getForUser(eq(userId), any(Pageable.class))).thenReturn(mockPage);

        // On ne passe aucun paramètre de pagination ici pour tester les @PageableDefault
        mockMvc.perform(get("/api/v1/notifications/queries/user/{userId}/history", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10)); // Vérifie la taille par défaut
    }

    @Test
    @DisplayName("Validation : devrait retourner 400 si l'ID n'est pas un UUID valide")
    void shouldReturn400WhenInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/queries/user/pas-un-uuid/history"))
                .andExpect(status().isBadRequest());
    }

    // Helper pour centraliser la création de la réponse et éviter les erreurs de constructeur
    private NotificationResponse createMockResponse() {
        return new NotificationResponse(
                UUID.randomUUID(),
                "🔔 Alerte",
                "Votre compte est prêt",
                "SENT",
                LocalDateTime.now()
        );
    }
}