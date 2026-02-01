package com.ivan.backend.infrastructure.adapter.identity;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.port.IdentityManagerPort;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KeycloakIdentityAdapter implements IdentityManagerPort {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public void createIdentity(User user, String password) {
        // 1. Préparation de la représentation utilisateur Keycloak
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(user.getEmail().value());
        userRep.setEmail(user.getEmail().value());
        userRep.setFirstName(user.getFirstName());
        userRep.setLastName(user.getLastName());
        userRep.setEnabled(true);
        
        // 2. Ajout des attributs personnalisés (L'ID de l'organisation)
        userRep.setAttributes(Map.of(
            "tenantId", Collections.singletonList(user.getTenantId().toString()),
            "role", Collections.singletonList(user.getRole().name())
        ));

        // 3. Définition du mot de passe
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(true); // L'utilisateur devra le changer à la 1ère connexion
        userRep.setCredentials(Collections.singletonList(credential));

        // 4. Appel à l'API Keycloak
        Response response = keycloak.realm(realm).users().create(userRep);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Erreur lors de la création Keycloak : " + response.getStatusInfo());
        }
    }
}