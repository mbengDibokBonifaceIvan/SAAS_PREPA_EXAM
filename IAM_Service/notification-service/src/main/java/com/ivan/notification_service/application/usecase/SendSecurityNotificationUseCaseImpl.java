package com.ivan.notification_service.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;

import lombok.RequiredArgsConstructor;

import com.ivan.notification_service.application.port.in.SendSecurityNotificationUseCase;
import com.ivan.notification_service.application.util.NotificationDispatcher;

@Service
@RequiredArgsConstructor
public class SendSecurityNotificationUseCaseImpl implements SendSecurityNotificationUseCase {
    private final NotificationDispatcher dispatcher;

    @Override
    @Transactional
    public void handle(UUID userId, String email, String name, String alertType, String detailedReason) {
        // 1. Détermination de la sévérité et de l'iconographie
        String emoji = "🔒";
        String actionHeader = "ALERTE DE SÉCURITÉ";

        if (alertType.contains("BANNED")) {
            emoji = "🚫";
            actionHeader = "COMPTE SUSPENDU";
        } else if (alertType.contains("RÉINITIALISATION")) {
            emoji = "🔑";
            actionHeader = "RÉINITIALISATION DE MOT DE PASSE";
        } else if (alertType.contains("LOCKED")) {
            emoji = "⚠️";
            actionHeader = "COMPTE VERROUILLÉ";
        }

        // 2. Construction d'un corps de message "Scannable"
        // On utilise des séparateurs et des libellés clairs
        String message = String.format(
                "Bonjour %s,\n\n" +
                "Nous avons détecté une activité importante concernant la sécurité de votre compte.\n\n" +
                "------------------------------------------\n" +
                "📌 ACTION : %s\n" +
                "📝 DÉTAIL : %s\n" +
                "------------------------------------------\n\n" +
                "🛡️ S'IL NE S'AGIT PAS DE VOUS :\n" +
                "Si vous n'êtes pas à l'origine de cette opération, votre compte est peut-être menacé. " +
                "Veuillez réinitialiser votre mot de passe immédiatement ou contacter notre support technique.\n\n" +
                "Besoin d'aide ? Répondez à ce mail ou visitez notre centre d'assistance.\n\n" +
                "Cordialement,\n" +
                "L'équipe Sécurité SAAS.",
                name, actionHeader, detailedReason);

        Notification notification = Notification.builder()
                .userId(userId)
                .recipient(email)
                .title(emoji + " " + actionHeader)
                .message(message)
                .type(NotificationType.EMAIL)
                .build();

        dispatcher.dispatch(notification);
    }
}