package com.ivan.notification_service.infrastructure.adapter.in.push;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.port.out.NotificationSender;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ToastPushNotificationAdapter implements NotificationSender {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Record interne pour la structure JSON du Front
    public record ToastPayload(String title, String message, String severity) {}

    @Override
    public void send(Notification notification) {
        SseEmitter emitter = emitters.get(notification.getUserId());
        if (emitter != null) {
            try {
                // Extraction de la sévérité : On récupère ce qui est entre crochets [SUCCESS]
                String title = notification.getTitle();
                String severity = "INFO"; // Par défaut
                
                if (title.contains("[") && title.contains("]")) {
                    severity = title.substring(title.indexOf("[") + 1, title.indexOf("]"));
                }

                log.info("🚀 ENVOI TOAST DYNAMIQUE : [User: {}] [Severity: {}]", 
                        notification.getUserId(), severity);
                
                emitter.send(SseEmitter.event()
                    .name("TOAST_EVENT")
                    .data(new ToastPayload(
                        notification.getTitle(), 
                        notification.getMessage(), 
                        severity // C'est maintenant dynamique !
                    )));
            } catch (IOException e) {
                emitters.remove(notification.getUserId());
            }
        }
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.PUSH;
    }

    public SseEmitter registerClient(UUID userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        this.emitters.put(userId, emitter);
        emitter.onCompletion(() -> this.emitters.remove(userId));
        emitter.onTimeout(() -> this.emitters.remove(userId));
        return emitter;
    }
}