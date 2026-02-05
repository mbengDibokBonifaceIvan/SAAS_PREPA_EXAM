package com.ivan.backend.application.usecase;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.exception.DomainException;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchUserUseCase {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<User> getDirectory(String requesterEmail, UUID optionalUnitId) {
        User requester = userRepository.findByEmail(new Email(requesterEmail))
                .orElseThrow(() -> new DomainException("Demandeur inconnu"));

        return switch (requester.getRole()) {
            case CENTER_OWNER -> (optionalUnitId != null)
                    ? userRepository.findAllByUnitIdAndTenantId(optionalUnitId, requester.getTenantId())
                    : userRepository.findAllByTenantId(requester.getTenantId());

            case UNIT_MANAGER -> userRepository.findAllByUnitId(requester.getUnitId());

            default -> List.of(requester);
        };
    }

    @Transactional(readOnly = true)
    public User getUserProfile(String email) {
        return userRepository.findByEmail(new Email(email))
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
    }
}
