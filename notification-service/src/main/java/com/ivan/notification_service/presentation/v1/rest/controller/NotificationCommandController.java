package com.ivan.notification_service.presentation.v1.rest.controller;

import com.ivan.notification_service.application.dto.FeedbackRequest;
import com.ivan.notification_service.application.dto.SecurityAlertRequest;
import com.ivan.notification_service.application.dto.UserNotificationRequest;
import com.ivan.notification_service.application.port.in.OnboardingUseCase;
import com.ivan.notification_service.application.port.in.ProcessGenericFeedbackUseCase;
import com.ivan.notification_service.application.port.in.SendSecurityNotificationUseCase;
import com.ivan.notification_service.infrastructure.adapter.in.push.ToastPushNotificationAdapter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/commands")
@RequiredArgsConstructor
@Tag(name = "Notification Commands", description = "Endpoints de commande pour l'envoi de notifications")
public class NotificationCommandController {

    private final SendSecurityNotificationUseCase securityUseCase;
    private final OnboardingUseCase onboardingUseCase;
    private final ProcessGenericFeedbackUseCase feedbackUseCase;
    private final ToastPushNotificationAdapter sseAdapter;

    @Operation(summary = "Alerte de sécurité", description = "Alerte suite à un verrouillage ou bannissement")
    @PostMapping("/security-alert")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendSecurityAlert(@RequestBody @Valid SecurityAlertRequest req) {
        securityUseCase.handle(req.userId(), req.email(), req.name(), req.alertType(), req.reason());
    }

    @Operation(summary = "Réinitialisation de mot de passe", description = "Alerte de demande de reset")
    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendPasswordReset(@RequestBody @Valid UserNotificationRequest req) {
        securityUseCase.handle(req.userId(), req.email(), req.name(), 
                "RÉINITIALISATION DE MOT DE PASSE", "Demande de modification reçue.");
    }

    @Operation(summary = "Bienvenue Organisation", description = "Email de bienvenue organisation")
    @PostMapping("/welcome-org")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendWelcomeOrg(@RequestBody @Valid UserNotificationRequest req) {
        onboardingUseCase.handleOrganizationWelcome(req.userId(), req.email(), req.name(), req.detail());
    }

    @Operation(summary = "Activation de compte", description = "Notification de compte actif")
    @PostMapping("/account-activation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendActivation(@RequestBody @Valid UserNotificationRequest req) {
        onboardingUseCase.handleAccountActivation(req.userId(), req.email(), req.name(), req.detail());
    }

    @Operation(summary = "Provisionnement utilisateur", description = "Alerte création de compte par admin")
    @PostMapping("/user-provisioned")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendProvisioning(@RequestBody @Valid UserNotificationRequest req) {
        onboardingUseCase.handleUserProvisioned(req.userId(), req.email(), req.name(), req.detail());
    }

    @Operation(summary = "Feedback générique", description = "Envoi manuel (Email, Push, etc.)")
    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendFeedback(@RequestBody @Valid FeedbackRequest req) {
        feedbackUseCase.handle(req);
    }

    @GetMapping(value = "/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable UUID userId) {
        return sseAdapter.registerClient(userId);
    }
}