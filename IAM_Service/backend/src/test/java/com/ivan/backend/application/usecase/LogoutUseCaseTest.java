package com.ivan.backend.application.usecase;

import com.ivan.backend.domain.exception.BusinessRuleViolationException;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class) // Initialise les mocks automatiquement
class LogoutUseCaseTest {

    @Mock
    private IdentityManagerPort identityManagerPort;

    @InjectMocks
    private LogoutUseCase logoutUseCase;

    @Test
    @DisplayName("Devrait réussir la déconnexion quand le token est valide")
    void shouldLogoutSuccessfully_WhenTokenIsValid() {
        // GIVEN
        String validToken = "valid-refresh-token";

        // WHEN
        logoutUseCase.execute(validToken);

        // THEN
        // On vérifie que le port de sortie a bien été appelé une fois avec le bon token
        verify(identityManagerPort, times(1)).logout(validToken);
    }

    @Test
    @DisplayName("Devrait lever une exception quand le token est null ou vide")
    void shouldThrowException_WhenTokenIsNullOrEmpty() {
        // GIVEN
        String emptyToken = "  ";

        // WHEN & THEN
        assertThrows(BusinessRuleViolationException.class, () -> {
            logoutUseCase.execute(emptyToken);
        });

        // On vérifie que le port de sortie n'a JAMAIS été appelé (car l'exception a coupé l'exécution)
        verifyNoInteractions(identityManagerPort);
    }

    @Test
    @DisplayName("Devrait logger l'erreur sans crasher si l'IAM échoue")
    void shouldLogAndNotCrash_WhenIdentityManagerFails() {
        // GIVEN
        String token = "some-token";
        // On simule une erreur technique venant de Keycloak
        doThrow(new RuntimeException("IAM Timeout")).when(identityManagerPort).logout(token);

        // WHEN
        logoutUseCase.execute(token);

        // THEN
        // On vérifie que l'appel a été tenté
        verify(identityManagerPort).logout(token);
        // L'app n'a pas crashé car le catch a géré l'erreur
    }
}