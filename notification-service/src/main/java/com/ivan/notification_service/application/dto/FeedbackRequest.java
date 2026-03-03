package com.ivan.notification_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Requête pour l'envoi de feedbacks ou notifications système directes")
public record FeedbackRequest(
    @Schema(description = "ID de l'utilisateur destinataire")
    @NotNull(message = "L'ID utilisateur est obligatoire")
    UUID userId,
    
    @Schema(description = "Titre court de la notification", example = "Mise à jour réussie")
    @NotBlank(message = "Le titre ne peut pas être vide")
    String title,
    
    @Schema(description = "Contenu détaillé du message", example = "Vos modifications ont été enregistrées avec succès.")
    @NotBlank(message = "Le message ne peut pas être vide")
    String message,
    
    @Schema(description = "Niveau de gravité pour l'affichage visuel", 
            example = "SUCCESS", 
            allowableValues = {"INFO", "WARNING", "SUCCESS", "ERROR"})
    @NotBlank(message = "La sévérité est obligatoire (INFO, WARNING, SUCCESS, ERROR)")
    String severity
) {}