package com.ivan.notification_service.infrastructure.adapter.out.mail;


import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.port.out.NotificationSender;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationAdapter implements NotificationSender {

    private final JavaMailSender mailSender;

    @Override
    public void send(Notification notification) {
        log.info("Envoi d'un email à : {}", notification.getRecipient());
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@votre-saas.com");
        message.setTo(notification.getRecipient());
        message.setSubject(notification.getTitle());
        message.setText(notification.getMessage());

        mailSender.send(message);
        log.info("Email envoyé avec succès via MailHog");
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }
}