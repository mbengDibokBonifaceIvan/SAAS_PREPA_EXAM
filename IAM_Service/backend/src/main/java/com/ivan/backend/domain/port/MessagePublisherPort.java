package com.ivan.backend.domain.port;

import com.ivan.backend.domain.event.OrganizationRegisteredEvent;

public interface MessagePublisherPort {
    void publishOrganizationRegistered(OrganizationRegisteredEvent event);
}