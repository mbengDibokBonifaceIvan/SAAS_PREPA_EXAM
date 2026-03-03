package com.ivan.backend.application.usecase;

import java.time.LocalDateTime;

import com.ivan.backend.domain.event.PasswordResetRequestedEvent;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.port.out.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.application.port.in.PasswordResetInputPort;
import com.ivan.backend.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PasswordResetUseCase implements PasswordResetInputPort {

    private final UserRepository userRepository;
    private final IdentityManagerPort identityManagerPort;
    private final MessagePublisherPort messagePublisher; // Pour RabbitMQ

    @Override
    public void requestReset(String email) {
        // On cherche en local d'abord
        userRepository.findByEmail(new Email(email))
            .filter(User::isActive) // Uniquement si le compte n'est pas banni
            .ifPresent(user -> {
                identityManagerPort.sendPasswordReset(email);
                messagePublisher.publishPasswordResetRequested(
                    new PasswordResetRequestedEvent(user.getId(), user.getFirstName(), user.getLastName(), email, LocalDateTime.now())
                );
            });
        
        // On ne jette aucune exception ici si l'utilisateur est absent.
    }
}
