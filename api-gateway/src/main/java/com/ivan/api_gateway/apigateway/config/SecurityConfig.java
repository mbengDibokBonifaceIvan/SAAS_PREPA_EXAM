package com.ivan.api_gateway.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable) // Désactivé pour les APIs stateless
            .authorizeExchange(exchanges -> exchanges
                // AJOUTE CETTE LIGNE : On autorise les checks de santé de Consul
                .pathMatchers("/actuator/**").permitAll()
                // On laisse passer l'auth (login, onboarding, forgot-password)
                .pathMatchers("/v1/auth/**").permitAll()
                // Tout le reste nécessite un JWT valide
                .pathMatchers("/favicon.ico").permitAll() // Évite l'erreur 401 console
            .anyExchange().permitAll()
                //.anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        
        return http.build();
    }
}