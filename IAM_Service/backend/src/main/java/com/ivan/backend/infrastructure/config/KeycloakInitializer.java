package com.ivan.backend.infrastructure.config;

import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KeycloakInitializer implements CommandLineRunner {

    private final Keycloak keycloak;
    @Value("${keycloak.realm}")
    private String realm;


    
    @Override
    public void run(String... args) {
        List<String> roles = List.of("CENTER_OWNER", "UNIT_MANAGER", "STAFF_MEMBER", "CANDIDATE");
        
        roles.forEach(roleName -> {
            try {
                keycloak.realm(realm).roles().get(roleName).toRepresentation();
            } catch (NotFoundException e) {
                RoleRepresentation role = new RoleRepresentation();
                role.setName(roleName);
                keycloak.realm(realm).roles().create(role);
            }
        });
    }
}