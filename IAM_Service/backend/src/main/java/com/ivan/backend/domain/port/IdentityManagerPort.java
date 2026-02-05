package com.ivan.backend.domain.port;

import com.ivan.backend.domain.entity.User;

public interface IdentityManagerPort {
    void createIdentity(User user, String password);
    void sendPasswordReset(String email);
    void disableIdentity(String email);             // Pour bannir
    void enableIdentity(String email);              // Pour activer
}