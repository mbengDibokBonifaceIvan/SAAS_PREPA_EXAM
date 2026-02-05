package com.ivan.backend.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.backend.application.port.ManageAccountInputPort;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.event.AccountActivatedEvent;
import com.ivan.backend.domain.event.AccountBannedEvent;
import com.ivan.backend.domain.port.IdentityManagerPort;
import com.ivan.backend.domain.port.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManageAccountUseCase implements ManageAccountInputPort {

    private final UserRepository userRepository;
    private final MessagePublisherPort messagePublisher;
    private final IdentityManagerPort identityManagerPort; // Port vers Keycloak

    @Override
    @Transactional
    public void banAccount(String userEmail, String ownerEmail) {
        User user = verifyAndGetRelativeUser(userEmail, ownerEmail);

        // 1. Action Locale
        user.deactivate();
        userRepository.save(user);

        // 2. Action Keycloak (Synchronisation)
        identityManagerPort.disableIdentity(userEmail);

        // 3. Notification
        messagePublisher.publishAccountBanned(new AccountBannedEvent(
                userEmail, "Banni manuellement par le chef de centre", ownerEmail, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void activateAccount(String userEmail, String ownerEmail) {
        User user = verifyAndGetRelativeUser(userEmail, ownerEmail);

        // 1. Action Locale
        user.activate();
        userRepository.save(user);

        // 2. Action Keycloak
        identityManagerPort.enableIdentity(userEmail);

        // 3. Notification
        messagePublisher.publishAccountActivated(new AccountActivatedEvent(
                userEmail, "Compte activé manuellement par le chef de centre", ownerEmail, LocalDateTime.now()));
    }

    private User verifyAndGetRelativeUser(String userEmail, String ownerEmail) {
        User owner = userRepository.findByEmail(new Email(ownerEmail))
                .orElseThrow(() -> new EntityNotFoundException("Propriétaire inconnu"));

        User user = userRepository.findByEmail(new Email(userEmail))
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur à gérer non trouvé"));

        if (!user.getTenantId().equals(owner.getTenantId())) {
            throw new SecurityException("Accès refusé : l'utilisateur n'appartient pas à votre organisation");
        }
        return user;
    }
}
