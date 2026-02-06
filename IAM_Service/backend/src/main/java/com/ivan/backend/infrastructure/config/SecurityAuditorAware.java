package com.ivan.backend.infrastructure.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("securityAuditorAware") // On force le nom ici
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM"); // Utile pour les actions automatiques ou l'onboarding public
        }

        // Retourne le 'subject' ou l'email contenu dans le JWT (getName() renvoie souvent le sub ou l'email selon ta config)
        return Optional.ofNullable(authentication.getName());
    }
}