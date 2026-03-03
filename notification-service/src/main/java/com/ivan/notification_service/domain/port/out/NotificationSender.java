package com.ivan.notification_service.domain.port.out;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;

public interface NotificationSender {
    void send(Notification notification);
    boolean supports(NotificationType type);
}