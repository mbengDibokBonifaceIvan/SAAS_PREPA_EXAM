package com.ivan.backend.domain.port;

import com.ivan.backend.domain.valueobject.AuthToken;
import com.ivan.backend.domain.valueobject.ProviderStatus;

public interface IdentityGatekeeper {
    
    // Authentification simple: Tente de récupérer un token auprès de Keycloak
    AuthToken authenticate(String email, String password);

    // Récupération des infos de statut depuis le fournisseur d'identité
    ProviderStatus getStatus(String email);

    void logout(String refreshToken);

}