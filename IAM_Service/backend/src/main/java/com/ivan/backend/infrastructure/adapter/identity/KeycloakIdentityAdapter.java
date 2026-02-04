package com.ivan.backend.infrastructure.adapter.identity;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.port.IdentityGatekeeper;
import com.ivan.backend.domain.port.IdentityManagerPort;
import com.ivan.backend.domain.valueobject.AuthToken;
import com.ivan.backend.domain.valueobject.ProviderStatus;
import com.ivan.backend.domain.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*; // Pour HttpHeaders, HttpEntity, ResponseEntity, MediaType

import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
// On implémente les DEUX interfaces pour que l'adapter soit complet
@Slf4j // Ajoute ceci !
public class KeycloakIdentityAdapter implements IdentityManagerPort, IdentityGatekeeper {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    private final RestTemplate restTemplate;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    // --- IMPLEMENTATION IDENTITY MANAGER (Creation) ---
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

        // ACTIONS REQUISES
        List<String> actions = new ArrayList<>();
        actions.add("VERIFY_EMAIL");

        // Si l'utilisateur doit changer son mot de passe (cas des employés créés par
        // l'owner)
        if (user.isMustChangePassword()) {
            actions.add("UPDATE_PASSWORD");
        }

        userRep.setRequiredActions(actions);

        // 2. Attributs personnalisés
        userRep.setAttributes(Map.of(
                "tenantId", Collections.singletonList(user.getTenantId().toString()),
                "role", Collections.singletonList(user.getRole().name())));

        // 3. Configuration du mot de passe
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(user.isMustChangePassword()); // Remplace "true" par la valeur du domaine !
        userRep.setCredentials(Collections.singletonList(credential));

        // 4. Exécution de la création
        try (Response response = keycloak.realm(realm).users().create(userRep)) {

            if (response.getStatus() == 201) {

                if (response.getLocation() == null) {
                    throw new KeycloakIdentityException(
                            "Keycloak a créé l'utilisateur mais n'a pas renvoyé son URI (Location header manquant)");
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
                throw new KeycloakIdentityException(
                        "Erreur Keycloak [" + response.getStatus() + "] : " + response.getStatusInfo());
            }
        } catch (Exception e) {
            throw new KeycloakIdentityException("Erreur lors de la création de l'identité dans Keycloak", e);
        }
    }

    private void assignRoleToUser(String userId, String roleName) {
        RoleRepresentation roleRep = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        keycloak.realm(realm).users().get(userId).roles().realmLevel()
                .add(Collections.singletonList(roleRep));
    }

    // --- IMPLEMENTATION IDENTITY GATEKEEPER (Login/Status) ---

    @Override
    public AuthToken authenticate(String email, String password) {

        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", authServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", email);
        body.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<?> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> resBody = (Map<String, Object>) response.getBody();

            if (resBody == null)
                throw new KeycloakIdentityException("Réponse Keycloak vide");

            return new AuthToken(
                    (String) resBody.get("access_token"),
                    (String) resBody.get("refresh_token"),
                    ((Number) resBody.get("expires_in")).longValue(),
                    (String) resBody.get("token_type"));
        } catch (HttpClientErrorException e) {
            log.error("DEBUG - Status Code: {}", e.getStatusCode());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.BAD_REQUEST) {

                // 1. On cherche l'ID de l'utilisateur Keycloak par son email
                List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email, true);

                if (!users.isEmpty()) {
                    String userId = users.get(0).getId();

                    // 2. On demande explicitement à Keycloak si cet ID est bloqué par le Brute
                    // Force
                    // Cette API renvoie l'état du verrouillage temporaire !
                    var bruteForceStatus = keycloak.realm(realm).attackDetection().bruteForceUserStatus(userId);

                    // "disabled" dans ce contexte signifie "verrouillé par brute force"
                    boolean isBruteForceLocked = (boolean) bruteForceStatus.get("disabled");

                    if (isBruteForceLocked || !users.get(0).isEnabled()) {
                        log.warn("BLOCAGE DÉTECTÉ via Admin API pour l'utilisateur : {}", email);
                        throw new AccountLockedException("Compte verrouillé (Brute Force)");
                    }
                }
            }
            throw new KeycloakIdentityException("Identifiants invalides");
        }
    }

    @Override
    public ProviderStatus getStatus(String email) {
        List<UserRepresentation> users = keycloak.realm(realm)
                .users()
                .searchByEmail(email, true);

        if (users.isEmpty()) {
            throw new KeycloakIdentityException("Utilisateur non trouvé dans Keycloak : " + email);
        }

        UserRepresentation kUser = users.get(0);

        boolean isEmailVerified = kUser.isEmailVerified();
        boolean mustChangePassword = kUser.getRequiredActions() != null &&
                kUser.getRequiredActions().contains("UPDATE_PASSWORD");

        return new ProviderStatus(isEmailVerified, mustChangePassword);
    }

    @Override
    public void logout(String refreshToken) {
        String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout", authServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(logoutUrl, request, Void.class);
        } catch (Exception e) {
            throw new KeycloakIdentityException("Erreur lors de la déconnexion : session introuvable ou déjà expirée");
        }
    }

    @Override
    public void sendPasswordReset(String email) {
        List<UserRepresentation> users = keycloak.realm(realm)
                .users().searchByEmail(email, true);

        if (!users.isEmpty()) {
            String userId = users.get(0).getId();
            // Action magique de Keycloak : génère le lien et envoie l'email
            keycloak.realm(realm).users().get(userId)
                    .executeActionsEmail(List.of("UPDATE_PASSWORD"));
        }
    }

}