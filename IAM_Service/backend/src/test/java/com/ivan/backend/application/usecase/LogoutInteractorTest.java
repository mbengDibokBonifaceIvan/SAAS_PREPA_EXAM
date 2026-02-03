package com.ivan.backend.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.ivan.backend.domain.port.IdentityGatekeeper;

@ExtendWith(MockitoExtension.class)
class LogoutInteractorTest {

    @Mock
    private IdentityGatekeeper identityGatekeeper;

    @InjectMocks
    private LogoutInteractor logoutInteractor;

    @Test
    void shouldInvokeGatekeeperWhenTokenIsProvided() {
        String token = "valid-refresh-token";

        logoutInteractor.execute(token);

        verify(identityGatekeeper, times(1)).logout(token);
    }

    @Test
    void shouldNotInvokeGatekeeperWhenTokenIsBlank() {
        // On vérifie que l'appel lance bien l'exception
        assertThrows(IllegalArgumentException.class, () -> {
            logoutInteractor.execute("");
        });

        // Et on vérifie que le gatekeeper n'a JAMAIS été appelé
        verify(identityGatekeeper, never()).logout(anyString());
    }
}
