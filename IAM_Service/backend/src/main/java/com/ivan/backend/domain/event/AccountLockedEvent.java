package com.ivan.backend.domain.event;

import java.time.LocalDateTime;

public record AccountLockedEvent(
    String email,
    String reason,
    LocalDateTime timestamp,
    String ownerEmail
) {}