package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.LoginRequest;
import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.application.mapper.LoginMapper;
import com.ivan.backend.application.port.in.LoginInputPort;
import com.ivan.backend.domain.event.AccountLockedEvent;
import com.ivan.backend.domain.exception.AccountLockedException;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.port.out.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginUseCase implements LoginInputPort {

    private final IdentityManagerPort identityManagerPort;
    private final UserRepository userRepository;
    private final MessagePublisherPort messagePublisher;

    @Override
    @Transactional(noRollbackFor = AccountLockedException.class)
    public LoginResponse login(LoginRequest request) {
        Email userEmail = new Email(request.email());

        try {
            // 1. Authentification auprès du fournisseur d'identité
            var token = identityManagerPort.authenticate(userEmail.value(), request.password());

            // 2. Récupération de l'utilisateur local
            // On utilise EntityNotFoundException ici car c'est une erreur technique : l'user est dans Keycloak mais pas chez nous
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new EntityNotFoundException("Utilisateur non synchronisé localement : " + userEmail.value()));

            // 3. Récupération des statuts frais depuis l'IAM
            var providerStatus = identityManagerPort.getStatus(userEmail.value());

            // 4. LOGIQUE DOMAINE : Synchronisation de l'état
            user.syncValidationStatus(providerStatus.isEmailVerified());
            user.updatePasswordRequirement(providerStatus.mustChangePassword());

            // 5. Persistance
            userRepository.save(user);

            log.info("Connexion réussie pour l'utilisateur : {}", userEmail.value());
            return LoginMapper.toResponse(user, token);

        } catch (AccountLockedException e) {
            handleAccountLockout(userEmail, e);
            throw e; 
        }
    }

    /**
     * Logique de gestion du verrouillage de compte (Brute force détecté par l'IAM)
     */
    private void handleAccountLockout(Email email, AccountLockedException e) {
        log.warn("Détection d'un compte verrouillé pour : {}", email.value());

        userRepository.findByEmail(email).ifPresent(user -> {
            // Action Domaine
            user.deactivate();
            userRepository.save(user);

            // Recherche du propriétaire pour notification
            String ownerEmail = userRepository.findByRoleAndTenantId(UserRole.CENTER_OWNER, user.getTenantId())
                    .map(owner -> owner.getEmail().value())
                    .orElse("admin@system.com");

            // Notification via Event
            messagePublisher.publishAccountLocked(new AccountLockedEvent(
                    user.getEmail().value(),
                    e.getMessage(),
                    LocalDateTime.now(),
                    ownerEmail));
        });
    }
}