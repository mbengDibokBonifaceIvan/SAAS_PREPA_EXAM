package com.ivan.notification_service.application.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.exception.NotificationDomainException;
import com.ivan.notification_service.domain.port.out.NotificationRepository;
import com.ivan.notification_service.domain.port.out.NotificationSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {
    private final NotificationRepository repository;
    private final List<NotificationSender> senders;

    public void dispatch(Notification notification) {
        try {
            repository.save(notification);
            
            NotificationSender sender = senders.stream()
                .filter(s -> s.supports(notification.getType()))
                .findFirst()
                .orElseThrow(() -> new NotificationDomainException("Sender non trouvé"));

            sender.send(notification);
            notification.markAsSent();
        } catch (Exception e) {
            log.error("Erreur d'envoi pour la notification {}", notification.getId(), e);
            notification.markAsFailed();
        } finally {
            repository.save(notification);
        }
    }
}
