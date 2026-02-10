package com.ivan.notification_service.domain.port.out;

import com.ivan.notification_service.domain.entity.Notification;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;


public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(UUID id);
    Page<Notification> findByUserId(UUID userId, Pageable pageable); // Doit retourner Page
}