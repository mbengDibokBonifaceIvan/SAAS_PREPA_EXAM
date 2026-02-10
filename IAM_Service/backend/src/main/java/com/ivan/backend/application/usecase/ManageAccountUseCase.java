package com.ivan.backend.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.backend.application.port.in.ManageAccountInputPort;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.event.AccountActivatedEvent;
import com.ivan.backend.domain.event.AccountBannedEvent;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.port.out.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageAccountUseCase implements ManageAccountInputPort {

    private final UserRepository userRepository;
    private final MessagePublisherPort messagePublisher;
    private final IdentityManagerPort identityManagerPort;

    @Override
    @Transactional
    public void banAccount(String userEmail, String requesterEmail) {
        User target = findUserByEmail(userEmail);
        User requester = findUserByEmail(requesterEmail);

        // 1. Validation métier centralisée dans l'entité User
        // Lance ResourceAccessDeniedException ou InsufficientPrivilegesException
        requester.checkCanManage(target);

        // 2. Action Locale
        target.deactivate();
        userRepository.save(target);

        // 3. Action Keycloak
        identityManagerPort.disableIdentity(userEmail);

        // 4. Notification: on va informer le target que son compte a été banni, et aussi qui est le responsable de cette action
        messagePublisher.publishAccountBanned(new AccountBannedEvent(target.getId(), target.getFirstName(), target.getLastName(), userEmail, "Banni par un administrateur", requesterEmail, LocalDateTime.now()));
        
        log.info("Compte banni : {} par {}", userEmail, requesterEmail);
    }

    @Override
    @Transactional
    public void activateAccount(String userEmail, String requesterEmail) {
        User target = findUserByEmail(userEmail);
        User requester = findUserByEmail(requesterEmail);

        // 1. Validation métier centralisée
        requester.checkCanManage(target);

        // 2. Action Locale
        target.activate();
        userRepository.save(target);

        // 3. Action Keycloak
        identityManagerPort.enableIdentity(userEmail);

        // 4. Notification
        messagePublisher.publishAccountActivated(new AccountActivatedEvent(target.getId(), target.getFirstName(), target.getLastName(),
                userEmail, "Activé par un administrateur", requesterEmail, LocalDateTime.now()));
        
        log.info("Compte activé : {} par {}", userEmail, requesterEmail);
    }

    /**
     * Helper pour éviter la répétition et utiliser l'objet Email
     */
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(new Email(email))
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé : " + email));
    }
}