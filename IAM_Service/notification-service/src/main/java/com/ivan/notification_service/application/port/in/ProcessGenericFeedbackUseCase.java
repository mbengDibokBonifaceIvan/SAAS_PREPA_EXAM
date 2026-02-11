package com.ivan.notification_service.application.port.in;

import com.ivan.notification_service.application.dto.FeedbackRequest;

// 3. Feedback Générique (Actions UI)

public interface ProcessGenericFeedbackUseCase {
    void handle(FeedbackRequest request);
}