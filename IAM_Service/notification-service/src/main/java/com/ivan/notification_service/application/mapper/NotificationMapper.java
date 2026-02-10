package com.ivan.notification_service.application.mapper;

import com.ivan.notification_service.application.dto.NotificationRequest;
import com.ivan.notification_service.application.dto.NotificationResponse;
import com.ivan.notification_service.domain.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    // Vers le Domaine (Utilisé par les envois)
    public Notification toDomain(NotificationRequest request) {
        return Notification.builder()
                .userId(request.userId())
                .recipient(request.recipient())
                .title(request.title())
                .message(request.message())
                .type(request.type())
                .build();
    }

    // Vers le DTO de sortie (Utilisé pour l'historique)
    public NotificationResponse toResponse(Notification domain) {
        return new NotificationResponse(
                domain.getId(),
                domain.getTitle(),
                domain.getMessage(),
                domain.getStatus().name(), // On transforme l'Enum en String
                domain.getCreatedAt()
        );
    }
}