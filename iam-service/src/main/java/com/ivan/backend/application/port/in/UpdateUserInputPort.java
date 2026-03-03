package com.ivan.backend.application.port.in;

import java.util.UUID;

import com.ivan.backend.application.dto.UpdateUserRequest;

public interface UpdateUserInputPort {
    void execute(UUID targetId, String requesterEmail, UpdateUserRequest request);    
}
