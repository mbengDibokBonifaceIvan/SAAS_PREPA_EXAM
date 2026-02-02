package com.ivan.backend.infrastructure.adapter.identity;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.port.IdentityManagerPort;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KeycloakIdentityAdapter implements IdentityManagerPort {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public void createIdentity(User user, String password) {
        // 1. Préparation de la représentation utilisateur
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(user.getEmail().value());
        userRep.setEmail(user.getEmail().value());
        userRep.setFirstName(user.getFirstName());
        userRep.setLastName(user.getLastName());
        userRep.setEnabled(true);
        userRep.setEmailVerified(user.isEmailVerified());

        // Action requise : Vérifier l'email
        userRep.setRequiredActions(List.of("VERIFY_EMAIL"));

        // 2. Attributs personnalisés
        userRep.setAttributes(Map.of(
                "tenantId", Collections.singletonList(user.getTenantId().toString()),
                "role", Collections.singletonList(user.getRole().name())));

        // 3. Configuration du mot de passe
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(true);
        userRep.setCredentials(Collections.singletonList(credential));

        // 4. Exécution de la création
        Response response = keycloak.realm(realm).users().create(userRep);

        if (response.getStatus() == 201) {
           
            if (response.getLocation() == null) {
                throw new RuntimeException("Keycloak a créé l'utilisateur mais n'a pas renvoyé son URI (Location header manquant)");
            }
            // Extraire l'ID de l'utilisateur à partir de l'URL retournée dans le Header
            // "Location"
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

            // Assigner le rôle Realm
            assignRoleToUser(userId, user.getRole().name());

            // Forcer l'envoi de l'email de vérification (nécessite SMTP configuré)
            keycloak.realm(realm).users().get(userId).sendVerifyEmail();

        } else {
            // Gestion d'erreur plus précise (ex: utilisateur déjà existant)
            throw new RuntimeException("Erreur Keycloak [" + response.getStatus() + "] : " + response.getStatusInfo());
        }
    }

    private void assignRoleToUser(String userId, String roleName) {
        RoleRepresentation roleRep = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        keycloak.realm(realm).users().get(userId).roles().realmLevel()
                .add(Collections.singletonList(roleRep));
    }
}