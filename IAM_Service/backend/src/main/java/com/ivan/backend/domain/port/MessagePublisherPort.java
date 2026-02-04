package com.ivan.backend.domain.port;

import com.ivan.backend.domain.event.AccountLockedEvent;
import com.ivan.backend.domain.event.OrganizationRegisteredEvent;
import com.ivan.backend.domain.event.PasswordResetRequestedEvent;

public interface MessagePublisherPort {
    void publishOrganizationRegistered(OrganizationRegisteredEvent event);
    void publishAccountLocked(AccountLockedEvent event);
    void publishPasswordResetRequested(PasswordResetRequestedEvent event);
}