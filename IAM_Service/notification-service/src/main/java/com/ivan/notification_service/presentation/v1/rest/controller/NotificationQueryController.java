package com.ivan.notification_service.presentation.v1.rest.controller;

import com.ivan.notification_service.application.dto.NotificationResponse;
import com.ivan.notification_service.application.port.in.GetNotificationHistoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/queries")
@RequiredArgsConstructor
public class NotificationQueryController {

    private final GetNotificationHistoryUseCase historyUseCase;

    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<NotificationResponse>> getHistory(@PathVariable UUID userId) {
        List<NotificationResponse> responses = historyUseCase.getForUser(userId);
        return responses.isEmpty() 
                ? ResponseEntity.noContent().build() 
                : ResponseEntity.ok(responses);
    }
}