package com.ivan.backend.application.usecase;

import com.ivan.backend.domain.port.IdentityGatekeeper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutInteractor implements LogoutUseCase {

    private final IdentityGatekeeper identityGatekeeper;

    @Override
    public void execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        identityGatekeeper.logout(refreshToken);
    }
}