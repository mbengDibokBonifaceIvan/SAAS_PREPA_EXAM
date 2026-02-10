package com.ivan.notification_service.application.usecase;


import java.util.List;
import java.util.UUID;

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
    public List<NotificationResponse> getForUser(UUID userId) {
        // Le repository devra avoir une méthode findByUserId
        return repository.findById(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
