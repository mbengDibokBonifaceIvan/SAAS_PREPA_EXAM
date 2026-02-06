package com.ivan.backend.domain.port.out;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.AuthToken;
import com.ivan.backend.domain.valueobject.ProviderStatus;

public interface IdentityManagerPort {
    void createIdentity(User user, String password);
    void sendPasswordReset(String email);
    void disableIdentity(String email);             // Pour bannir
    void enableIdentity(String email);              // Pour activer
    void updateUserRole(String email, String roleName);
    AuthToken authenticate(String email, String password);
    ProviderStatus getStatus(String email);
    void logout(String refreshToken);
}