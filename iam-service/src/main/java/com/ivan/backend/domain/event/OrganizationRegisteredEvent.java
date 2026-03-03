package com.ivan.backend.domain.event;

import java.util.UUID;

public record OrganizationRegisteredEvent(
    UUID organizationId,
    String organizationName,
    UUID ownerId,
    String ownerEmail,
    String ownerFirstName,
    String ownerLastName,
    Boolean isOwnerEmailVerified
) {}
