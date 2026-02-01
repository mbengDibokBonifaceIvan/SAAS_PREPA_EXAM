package com.ivan.backend.domain.port;

import com.ivan.backend.domain.entity.User;

public interface IdentityManagerPort {
    void createIdentity(User user, String password);
}