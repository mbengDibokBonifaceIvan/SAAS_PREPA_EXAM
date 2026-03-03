package com.ivan.backend.application.usecase;

import com.ivan.backend.application.port.in.LogoutInputPort;
import com.ivan.backend.domain.exception.BusinessRuleViolationException;
import com.ivan.backend.domain.port.out.IdentityManagerPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutUseCase  implements LogoutInputPort{

    private final IdentityManagerPort identityManagerPort;

    /**
     * Termine la session de l'utilisateur en révoquant le refresh token.
     * @param refreshToken Le token de rafraîchissement à invalider.
     */
    @Override
    public void execute(String refreshToken) {
        // 1. Validation de l'entrée via une exception du domaine
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessRuleViolationException("Le refresh token est obligatoire pour la déconnexion.");
        }

        try {
            // 2. Appel au port IdentityManager pour l'action technique (Keycloak)
            identityManagerPort.logout(refreshToken);
            log.info("Déconnexion réussie pour le token fourni.");
            
        } catch (Exception e) {
            // On log l'erreur mais on ne bloque pas forcément le client 
            // (une session expirée côté serveur est déjà une forme de déconnexion)
            log.error("Erreur lors de la révocation du token dans l'IAM : {}", e.getMessage());
        }
    }
}