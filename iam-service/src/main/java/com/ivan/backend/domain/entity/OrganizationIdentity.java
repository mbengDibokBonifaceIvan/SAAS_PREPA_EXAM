package com.ivan.backend.domain.entity;

import java.util.UUID;

public record OrganizationIdentity(UUID id, String name) {
    public OrganizationIdentity {
        if (id == null) id = UUID.randomUUID();
    }
}
