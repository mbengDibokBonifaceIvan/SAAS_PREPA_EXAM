package com.ivan.backend.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountActivatedEvent(UUID userId, String firstName, String lastName, String userEmail, String reason, String ownerEmail, LocalDateTime timestamp) {}
