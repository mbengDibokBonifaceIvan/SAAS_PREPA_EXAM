package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.LoginRequest;
import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.application.mapper.LoginMapper;
import com.ivan.backend.application.port.LoginInputPort;
import com.ivan.backend.domain.event.AccountLockedEvent; // Ton record d'événement
import com.ivan.backend.domain.exception.AccountLockedException;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.port.IdentityGatekeeper;
import com.ivan.backend.domain.port.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginUseCase implements LoginInputPort {

    private final IdentityGatekeeper identityGatekeeper;
    private final UserRepository userRepository;
    private final MessagePublisherPort messagePublisher;

    @Override
    @Transactional(noRollbackFor = AccountLockedException.class) // AJOUTE CECI
    public LoginResponse login(LoginRequest request) {

        try { // 1. Authentification auprès de Keycloak (Port de sortie)
            var token = identityGatekeeper.authenticate(request.email(), request.password());

            // 2. Récupération de l'utilisateur local
            User user = userRepository.findByEmail(new Email(request.email()))
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé localement"));

            // 3. Récupération des statuts frais (Email vérifié ? Password à changer ?)
            var providerStatus = identityGatekeeper.getStatus(request.email());

            // 4. LOGIQUE DOMAINE : Synchronisation et Activation
            user.syncValidationStatus(providerStatus.isEmailVerified());
            user.updatePasswordRequirement(providerStatus.mustChangePassword());

            // 5. Persistance de l'état mis à jour
            userRepository.save(user);

            // 6. Retourne la réponse mappée
            return LoginMapper.toResponse(user, token);
        } catch (AccountLockedException e) {
            // --- GESTION DU BLOCAGE NATIF KEYCLOAK ---

            // On récupère l'user pour mettre à jour la base locale et notifier
            User user = userRepository.findByEmail(new Email(request.email()))
                    .orElseThrow(() -> e); // Si pas d'user local, on relance l'exception d'origine

            // Désactivation locale
            user.deactivate();
            userRepository.save(user);

            // 3. Recherche de l'email du propriétaire (Owner)
            String ownerEmail = userRepository.findByRoleAndTenantId(UserRole.CENTER_OWNER, user.getTenantId())
                    .map(owner -> owner.getEmail().value())
                    .orElse("admin@system.com"); // Fallback si pas de owner trouvé

            // Notification RabbitMQ
            AccountLockedEvent event = new AccountLockedEvent(
                    user.getEmail().value(),
                    "Compte verrouillé suite à 5 tentatives infructueuses (Protection Brute Force)",
                    LocalDateTime.now(),
                    ownerEmail);

            // Appel via le port
            messagePublisher.publishAccountLocked(event);
            // On relance l'exception pour que le Controller renvoie une 403 ou 423
            throw e;
        }
    }
}