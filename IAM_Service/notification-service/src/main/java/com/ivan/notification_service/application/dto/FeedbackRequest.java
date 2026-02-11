package com.ivan.notification_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FeedbackRequest(
    @NotNull(message = "L'ID utilisateur est obligatoire")
    UUID userId,
    
    @NotBlank(message = "Le titre ne peut pas être vide")
    String title,
    
    @NotBlank(message = "Le message ne peut pas être vide")
    String message,
    
    @NotBlank(message = "La sévérité est obligatoire (INFO, WARNING, SUCCESS, ERROR)")
    String severity
) {}