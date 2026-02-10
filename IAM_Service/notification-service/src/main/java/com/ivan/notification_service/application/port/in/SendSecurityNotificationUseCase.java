package com.ivan.notification_service.application.port.in;

import java.util.UUID;

// 1. Sécurité (Account Locked/Banned)
public interface SendSecurityNotificationUseCase {
    void handle(UUID userId, String email, String alertType);
}
