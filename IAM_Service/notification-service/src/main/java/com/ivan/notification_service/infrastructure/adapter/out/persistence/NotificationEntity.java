package com.ivan.notification_service.infrastructure.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ivan.notification_service.domain.valueobject.NotificationStatus;
import com.ivan.notification_service.domain.valueobject.NotificationType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notifications", indexes = {
    @jakarta.persistence.Index(name = "idx_user_id", columnList = "userId")
})
@Getter @Setter
public class NotificationEntity {
    @Id
    private UUID id;
    private UUID userId;
    private String recipient;
    private String title;
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    private LocalDateTime createdAt;
}
