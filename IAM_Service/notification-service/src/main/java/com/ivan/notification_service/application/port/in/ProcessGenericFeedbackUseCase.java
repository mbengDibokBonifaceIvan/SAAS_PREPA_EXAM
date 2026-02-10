package com.ivan.notification_service.application.port.in;

import java.util.UUID;


// 3. Feedback Générique (Actions UI)
public interface ProcessGenericFeedbackUseCase {
    /**
     * @param severity : SUCCESS, INFO, WARNING, ERROR (utilisé pour la couleur du Toast)
     */
    void handle(UUID userId, String title, String message, String severity);
}