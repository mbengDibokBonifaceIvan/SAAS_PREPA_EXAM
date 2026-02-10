package com.ivan.notification_service.domain.entity;

import com.ivan.notification_service.domain.exception.InvalidNotificationException;
import com.ivan.notification_service.domain.valueobject.NotificationStatus;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Notification {
    private final UUID id;
    private final UUID userId;
    private final String recipient;
    private final String title;
    private final String message;
    private final NotificationType type;
    private NotificationStatus status;
    private final LocalDateTime createdAt;

    @Builder
    public Notification(UUID id, UUID userId, String recipient, String title, String message, NotificationType type) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId;
        this.recipient = recipient;
        this.title = title;
        this.message = message;
        this.type = type;
        this.status = NotificationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        selfValidate();
    }

    private void selfValidate() {
        if (userId == null) throw new InvalidNotificationException("User ID cannot be null");
        if (recipient == null || recipient.isBlank()) throw new InvalidNotificationException("Recipient cannot be empty");
        if (type == null) throw new InvalidNotificationException("Notification type must be specified");
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
    }

    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
    }
}