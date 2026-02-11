package com.ivan.notification_service.presentation.v1.rest.controller;

import com.ivan.notification_service.application.dto.FeedbackRequest;
import com.ivan.notification_service.application.port.in.OnboardingUseCase;
import com.ivan.notification_service.application.port.in.ProcessGenericFeedbackUseCase;
import com.ivan.notification_service.application.port.in.SendSecurityNotificationUseCase;
import com.ivan.notification_service.infrastructure.adapter.in.push.ToastPushNotificationAdapter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@Tag(name = "Notification Commands", description = "Endpoints pour déclencher l'envoi de notifications manuellement")
public class NotificationCommandController {

    private final SendSecurityNotificationUseCase securityUseCase;
    private final OnboardingUseCase onboardingUseCase;
    private final ProcessGenericFeedbackUseCase feedbackUseCase;
    private final ToastPushNotificationAdapter sseAdapter;

    @Operation(summary = "Alerte de sécurité", description = "Envoie une notification suite à un verrouillage ou bannissement")
    @PostMapping("/security-alert")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendSecurityAlert(
            @RequestParam UUID userId,
            @RequestParam String email,
            @RequestParam String name,
            @Parameter(description = "Ex: USER_LOCKED, ACCOUNT_BANNED") @RequestParam String alertType,
            @RequestParam String reason) {
        securityUseCase.handle(userId, email, name, alertType, reason);
    }

    @Operation(summary = "Réinitialisation de mot de passe", description = "Alerte l'utilisateur qu'une demande de reset a été effectuée")
    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendPasswordReset(
            @RequestParam UUID userId,
            @RequestParam String email,
            @RequestParam String name) {
        securityUseCase.handle(userId, email, name, "RÉINITIALISATION DE MOT DE PASSE",
                "Une demande de modification de vos identifiants a été reçue.");
    }

    @Operation(summary = "Bienvenue Organisation", description = "Envoie l'email de bienvenue après la création d'une organisation")
    @PostMapping("/welcome-org")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendWelcomeOrg(@RequestParam UUID userId, @RequestParam String email, @RequestParam String name,
            @RequestParam String orgName) {
        onboardingUseCase.handleOrganizationWelcome(userId, email, name, orgName);
    }

    @Operation(summary = "Activation de compte", description = "Informe l'utilisateur que son compte est désormais actif")
    @PostMapping("/account-activation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendActivation(@RequestParam UUID userId, @RequestParam String email, @RequestParam String name,
            @RequestParam String reason) {
        onboardingUseCase.handleAccountActivation(userId, email, name, reason);
    }

    @Operation(summary = "Provisionnement utilisateur", description = "Informe un collaborateur qu'un compte lui a été créé par un administrateur")
    @PostMapping("/user-provisioned")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendProvisioning(@RequestParam UUID userId, @RequestParam String email, @RequestParam String name,
            @RequestParam String role) {
        onboardingUseCase.handleUserProvisioned(userId, email, name, role);
    }

    @ApiResponse(responseCode = "202", description = "Demande d'envoi acceptée")
    @Operation(summary = "Feedback générique", description = "Envoi d'une notification via un objet JSON")
    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendFeedback(@RequestBody @Valid FeedbackRequest request) {
        feedbackUseCase.handle(request);
    }

    @GetMapping(value = "/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable UUID userId) {
        return sseAdapter.registerClient(userId);
    }

}