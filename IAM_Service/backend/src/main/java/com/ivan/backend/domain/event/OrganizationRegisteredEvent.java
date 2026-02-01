package com.ivan.backend.domain.event;

import java.util.UUID;

public record OrganizationRegisteredEvent(
    UUID organizationId,
    String organizationName,
    String ownerEmail,
    String ownerFirstName,
    String ownerLastName
) {}
