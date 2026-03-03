package com.ivan.notification_service.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;

import lombok.RequiredArgsConstructor;

import com.ivan.notification_service.application.port.in.OnboardingUseCase;
import com.ivan.notification_service.application.util.NotificationDispatcher;

@Service
@RequiredArgsConstructor
public class OnboardingUseCaseImpl implements OnboardingUseCase {
    private final NotificationDispatcher dispatcher;

    @Override
    @Transactional
    public void handleOrganizationWelcome(UUID userId, String email, String name, String orgName) {
        // Un titre qui valorise l'utilisateur
        String title = String.format("🚀 Bienvenue à bord, %s !", orgName);

        String body = String.format(
                "Bonjour %s,\n\n" +
                        "C'est un grand jour ! Votre organisation '%s' est désormais officiellement configurée sur notre plateforme.\n\n"
                        +
                        "Voici vos premières étapes :\n" +
                        " 1️⃣ Explorez votre nouveau tableau de bord.\n" +
                        " 2️⃣ Invitez vos collaborateurs à rejoindre l'aventure.\n" +
                        " 3️⃣ Configurez vos premiers services en quelques clics.\n\n" +
                        "Nous sommes impatients de voir ce que vous allez accomplir avec nous.\n\n" +
                        "L'équipe Succès Client.",
                name, orgName);

        send(userId, email, title, body);
    }

    @Override
    @Transactional
    public void handleAccountActivation(UUID userId, String email, String name, String context) {
        // On remplace le terme technique "context" par une formulation fluide
        String body = String.format(
                "Bonjour %s,\n\n" +
                        "Excellente nouvelle : votre compte est désormais entièrement activé ! ✨\n\n" +
                        "Détails de l'activation :\n" +
                        "• Motif : %s\n" +
                        "• Accès : Illimité à toutes les fonctionnalités\n\n" +
                        "Vous n'avez plus aucune restriction. Profitez pleinement de votre espace personnel dès maintenant.\n\n"
                        +
                        "À très vite,\n" +
                        "L'équipe Support.",
                name, context);

        send(userId, email, "✅ Votre compte est opérationnel", body);
    }

    @Override
    @Transactional
    public void handleUserProvisioned(UUID userId, String email, String name, String role) {
        // UX : On insiste sur le fait que l'accès a été créé par l'organisation
        String title = "🎉 Invitation à rejoindre la plateforme";
        String body = String.format(
                "Bonjour %s,\n\n" +
                        "Un compte collaborateur vient de vous être préparé.\n\n" +
                        "Détails de votre accès :\n" +
                        "• Rôle attribué : %s\n" +
                        "• Statut : Prêt à l'emploi\n\n" +
                        "Vous pouvez vous connecter en utilisant votre adresse email professionnelle. " +
                        "Si vous n'avez pas encore de mot de passe, utilisez la procédure 'Mot de passe oublié'.\n\n" +
                        "Bienvenue parmi nous !",
                name, role);

        send(userId, email, title, body);
    }

    /**
     * Centralisation de la logique de création et d'envoi.
     * Si tu veux passer en HTML plus tard, tu ne modifieras que cette méthode.
     */
    private void send(UUID userId, String email, String title, String body) {
        Notification notification = Notification.builder()
                .userId(userId)
                .recipient(email)
                .title(title)
                .message(body)
                .type(NotificationType.EMAIL)
                .build();

        dispatcher.dispatch(notification);
    }
}