package com.ivan.backend.domain.event;

import java.time.LocalDateTime;

public record AccountActivatedEvent(String userEmail, String reason, String ownerEmail, LocalDateTime timestamp) {}
