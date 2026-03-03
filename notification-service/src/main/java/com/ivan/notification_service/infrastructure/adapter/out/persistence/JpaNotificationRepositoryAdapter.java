package com.ivan.notification_service.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.port.out.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaNotificationRepositoryAdapter implements NotificationRepository {
    // Utilise le nom exact de l'interface créée à l'étape 1
    private final NotificationJpaRepository jpaRepository;
    private final PersistenceMapper mapper;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = mapper.toEntity(notification);
        NotificationEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Notification> findByUserId(UUID userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable)
                .map(mapper::toDomain); // Ici .map() fonctionne car jpaRepository retourne une Page
    }
}