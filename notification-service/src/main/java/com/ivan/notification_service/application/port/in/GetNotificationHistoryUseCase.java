package com.ivan.notification_service.application.port.in;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ivan.notification_service.application.dto.NotificationResponse;

// 4. Consultation (Historique)
public interface GetNotificationHistoryUseCase {
    // Utiliser Pageable permet de gérer le tri et la taille du lot
    Page<NotificationResponse> getForUser(UUID userId, Pageable pageable);
}
