package com.ivan.backend.application.port;

import com.ivan.backend.application.dto.ProvisionUserRequest;

public interface ProvisionUserInputPort {
    void execute(ProvisionUserRequest request, String creatorEmail);
}