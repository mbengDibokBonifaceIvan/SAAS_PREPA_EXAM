package com.ivan.backend.application.port.in;

import com.ivan.backend.domain.entity.User;
import java.util.List;
import java.util.UUID;

public interface SearchUserInputPort {
    List<User> getDirectory(String requesterEmail, UUID optionalUnitId);
    User getUserById(UUID id, String requesterEmail);
    User getUserProfile(String email);
}