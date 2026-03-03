package com.ivan.backend.infrastructure.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("securityAuditorAware") // On force le nom ici
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. On vérifie si l'auth existe, est authentifiée ET n'est pas anonyme
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return Optional.of("SYSTEM");
        }

        // 2. Extraction du JWT
        // Dans SecurityAuditorAware.java
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // On stocke le SUB (UUID de Keycloak) pour la stabilité
            return Optional.ofNullable(jwt.getSubject());
        }

        // Retourne le 'subject' ou l'email contenu dans le JWT (getName() renvoie
        // souvent le sub ou l'email selon ta config)
        return Optional.ofNullable(authentication.getName());
    }
}