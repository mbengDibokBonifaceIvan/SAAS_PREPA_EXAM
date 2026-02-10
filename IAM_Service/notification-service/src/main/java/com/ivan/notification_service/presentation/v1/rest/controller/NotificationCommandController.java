package com.ivan.notification_service.presentation.v1.rest.controller;

import com.ivan.notification_service.application.dto.NotificationRequest;
import com.ivan.notification_service.application.port.in.ProcessGenericFeedbackUseCase;
import com.ivan.notification_service.application.port.in.SendOnboardingWelcomeUseCase;
import com.ivan.notification_service.application.port.in.SendSecurityNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/commands")
@RequiredArgsConstructor
public class NotificationCommandController {

    private final SendSecurityNotificationUseCase securityUseCase;
    private final SendOnboardingWelcomeUseCase onboardingUseCase;
    private final ProcessGenericFeedbackUseCase feedbackUseCase;

    @PostMapping("/security-alert")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendSecurityAlert(@RequestParam UUID userId, @RequestParam String email, @RequestParam String reason) {
        securityUseCase.handle(userId, email, reason);
    }

    @PostMapping("/welcome")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendWelcome(@RequestParam UUID userId, @RequestParam String email, @RequestParam String name) {
        onboardingUseCase.handle(userId, email, name);
    }

    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendFeedback(@RequestBody NotificationRequest request) {
        feedbackUseCase.handle(
            request.userId(), 
            request.recipient(), 
            request.title(), 
            request.message(), 
            request.type()
        );
    }
}