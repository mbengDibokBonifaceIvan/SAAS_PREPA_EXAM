package com.ivan.notification_service.application.port.in;

import java.util.UUID;

// 2. Bienvenue (Organization/User Registered)
public interface SendOnboardingWelcomeUseCase {
    void handle(UUID userId, String email, String name);
}
