package com.ivan.notification_service.application.port.in;

import java.util.List;
import java.util.UUID;

import com.ivan.notification_service.application.dto.NotificationResponse;

// 4. Consultation (Historique)
public interface GetNotificationHistoryUseCase {
    List<NotificationResponse> getForUser(UUID userId);
}
