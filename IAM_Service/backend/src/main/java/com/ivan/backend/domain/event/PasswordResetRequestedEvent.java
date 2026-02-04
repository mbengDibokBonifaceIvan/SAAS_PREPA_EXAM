package com.ivan.backend.domain.event;

import java.time.LocalDateTime;

public record PasswordResetRequestedEvent(
    String email, 
    LocalDateTime requestedAt
) {}
