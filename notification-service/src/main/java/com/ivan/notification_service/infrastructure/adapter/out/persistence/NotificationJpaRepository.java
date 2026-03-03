package com.ivan.notification_service.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {
    // Spring Data JPA gère automatiquement le Pageable
    Page<NotificationEntity> findByUserId(UUID userId, Pageable pageable);
}

