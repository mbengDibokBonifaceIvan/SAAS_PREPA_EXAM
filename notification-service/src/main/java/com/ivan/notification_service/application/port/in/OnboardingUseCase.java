package com.ivan.notification_service.application.port.in;

import java.util.UUID;

public interface OnboardingUseCase {
    // Cas 1 : Création d'organisation
    void handleOrganizationWelcome(UUID userId, String email, String name, String orgName);
    
    // Cas 2 : Activation de compte
    void handleAccountActivation(UUID userId, String email, String name, String reason);
    
    // Cas 3 : Nouvel utilisateur provisionné (Ton nouveau cas !)
    void handleUserProvisioned(UUID userId, String email, String name, String role);
}
