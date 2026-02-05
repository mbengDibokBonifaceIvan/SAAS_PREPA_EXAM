package com.ivan.backend.application.usecase;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.exception.DomainException;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

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
    public User getUserById(UUID id, String requesterEmail) {
        User requester = findUserByEmail(requesterEmail);
        User target = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID : " + id));

        checkAccessRights(requester, target);
        return target;
    }

    // Extraction de la logique de sécurité pour réutilisation
    private void checkAccessRights(User requester, User target) {
        if (!target.getTenantId().equals(requester.getTenantId())) {
            throw new DomainException("Accès refusé : périmètre organisationnel différent");
        }

        if (requester.getRole() == UserRole.UNIT_MANAGER && 
            (target.getUnitId() == null || !target.getUnitId().equals(requester.getUnitId()))) {
            throw new DomainException("Accès refusé : cet utilisateur n'est pas dans votre unité");
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(new Email(email))
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur inconnu : " + email));
    }
    
    @Transactional(readOnly = true)
    public User getUserProfile(String email) {
        return userRepository.findByEmail(new Email(email))
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
    }
}
