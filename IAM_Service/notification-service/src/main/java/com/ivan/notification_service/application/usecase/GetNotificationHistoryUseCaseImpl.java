package com.ivan.notification_service.application.usecase;


import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.notification_service.domain.port.out.NotificationRepository;

import lombok.RequiredArgsConstructor;

import com.ivan.notification_service.application.dto.NotificationResponse;
import com.ivan.notification_service.application.mapper.NotificationMapper;
import com.ivan.notification_service.application.port.in.GetNotificationHistoryUseCase;



@Service
@RequiredArgsConstructor
public class GetNotificationHistoryUseCaseImpl implements GetNotificationHistoryUseCase {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getForUser(UUID userId, Pageable pageable) {
        // Le repository doit maintenant accepter un Pageable
        return repository.findByUserId(userId, pageable)
                .map(mapper::toResponse);
    }
}
