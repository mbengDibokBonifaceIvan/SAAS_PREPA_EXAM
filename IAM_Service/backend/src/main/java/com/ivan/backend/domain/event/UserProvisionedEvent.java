package com.ivan.backend.domain.event;

import com.ivan.backend.domain.valueobject.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserProvisionedEvent(
    UUID userId,
    String firstName, 
    String lastName,
    String userEmail,
    UserRole role,
    UUID tenantId,
    LocalDateTime occurredAt
) {}
