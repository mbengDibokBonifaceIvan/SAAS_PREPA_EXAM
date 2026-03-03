package com.ivan.notification_service.infrastructure.adapter.out.persistence;

import com.ivan.notification_service.domain.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class PersistenceMapper {

    public NotificationEntity toEntity(Notification domain) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setRecipient(domain.getRecipient());
        entity.setTitle(domain.getTitle());
        entity.setMessage(domain.getMessage());
        entity.setType(domain.getType());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public Notification toDomain(NotificationEntity entity) {
        return Notification.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .recipient(entity.getRecipient())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                // Le builder du domaine gère le status et date via le constructeur, 
                // mais on peut ajouter des setters ou un constructeur spécifique si besoin
                .build();
    }
}