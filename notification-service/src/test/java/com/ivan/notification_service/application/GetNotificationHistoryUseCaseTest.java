package com.ivan.notification_service.application;

import com.ivan.notification_service.application.dto.NotificationResponse;
import com.ivan.notification_service.application.mapper.NotificationMapper;
import com.ivan.notification_service.application.usecase.GetNotificationHistoryUseCaseImpl;
import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.port.out.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetNotificationHistoryUseCaseTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationMapper mapper;

    @InjectMocks
    private GetNotificationHistoryUseCaseImpl useCase;

    @Test
    @DisplayName("Devrait retourner une page de NotificationResponse pour un utilisateur donné")
    void shouldReturnPagedNotificationsForUser() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        // On mock une entité du domaine
        Notification notification = mock(Notification.class);
        Page<Notification> entityPage = new PageImpl<>(List.of(notification));

        // On mock la réponse attendue
        NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(),
                "Titre Test",
                "Message Test",
                "SENT",
                LocalDateTime.now());
        // Configuration des mocks
        when(repository.findByUserId(userId, pageable)).thenReturn(entityPage);
        when(mapper.toResponse(notification)).thenReturn(response);

        // WHEN
        Page<NotificationResponse> result = useCase.getForUser(userId, pageable);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(response, result.getContent().get(0));

        // Vérification des interactions
        verify(repository, times(1)).findByUserId(userId, pageable);
        verify(mapper, times(1)).toResponse(any(Notification.class));
    }

    @Test
    @DisplayName("Devrait retourner une page vide si l'utilisateur n'a pas de notifications")
    void shouldReturnEmptyPageWhenNoNotificationsFound() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findByUserId(userId, pageable)).thenReturn(Page.empty());

        // WHEN
        Page<NotificationResponse> result = useCase.getForUser(userId, pageable);

        // THEN
        assertTrue(result.isEmpty());
        verify(repository).findByUserId(userId, pageable);
        verifyNoInteractions(mapper); // Le mapper ne doit pas être appelé si la page est vide
    }
}
