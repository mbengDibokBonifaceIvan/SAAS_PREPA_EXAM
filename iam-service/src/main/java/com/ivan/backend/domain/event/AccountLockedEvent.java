package com.ivan.backend.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountLockedEvent(
    UUID userId,
    String firstName, 
    String lastName,
    String userEmail,
    String reason,
    LocalDateTime timestamp,
    String ownerEmail
) {}