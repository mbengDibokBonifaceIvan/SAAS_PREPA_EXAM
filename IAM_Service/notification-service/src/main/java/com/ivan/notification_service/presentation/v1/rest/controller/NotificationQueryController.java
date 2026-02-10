package com.ivan.notification_service.presentation.v1.rest.controller;

import com.ivan.notification_service.application.dto.NotificationResponse;
import com.ivan.notification_service.application.port.in.GetNotificationHistoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/queries")
@RequiredArgsConstructor
@Tag(name = "Notification Queries", description = "Endpoints pour la consultation et l'historique des notifications")
public class NotificationQueryController {

    private final GetNotificationHistoryUseCase historyUseCase;

    @Operation(
        summary = "Consulter l'historique d'un utilisateur",
        description = "Récupère une page de notifications pour un utilisateur spécifique. Supporte la pagination et le tri (ex: ?page=0&size=10&sort=createdAt,desc)"
    )
    @ApiResponse(responseCode = "200", description = "Historique récupéré avec succès")
    @ApiResponse(responseCode = "204", description = "Aucune notification trouvée pour cet utilisateur", content = @Content)
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<Page<NotificationResponse>> getHistory(
            @Parameter(description = "ID unique de l'utilisateur (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,
            
            @Parameter(hidden = true) // On cache l'objet Pageable brut pour ne pas encombrer Swagger
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<NotificationResponse> page = historyUseCase.getForUser(userId, pageable);
        
        return page.isEmpty() 
                ? ResponseEntity.noContent().build() 
                : ResponseEntity.ok(page);
    }
}