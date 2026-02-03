package com.ivan.backend.infrastructure.adapter.persistence;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository repository;

    @Override
    public User save(User user) {
        // On cherche l'entité existante pour faire un vrai UPDATE
        UserEntity entity = repository.findById(user.getId())
                .orElse(new UserEntity());

        entity.setUserId(user.getId());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setEmail(user.getEmail().value());
        entity.setRole(user.getRole());
        entity.setExternalOrganizationId(user.getTenantId());
        // On mappe le champ même s'il est null dans le domaine pour l'instant
        entity.setExternalUnitId(null);
        entity.setEmailVerified(user.isEmailVerified());
        entity.setActive(user.isActive());
        entity.setMustChangePassword(user.isMustChangePassword());
        repository.save(entity);
        return user;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value())
                .map(entity -> new User(
                        entity.getUserId(),
                        entity.getFirstName(),
                        entity.getLastName(),
                        new Email(entity.getEmail()),
                        entity.getExternalOrganizationId(),
                        entity.getExternalUnitId(),
                        entity.getRole(),
                        entity.isEmailVerified(),
                        entity.isActive(),
                        entity.isMustChangePassword()));
    }

    @Override
    public Optional<User> findByRoleAndTenantId(UserRole role, UUID tenantId) {
        return repository.findByRoleAndExternalOrganizationId(role, tenantId)
                .map(entity -> new User(
                        entity.getUserId(),
                        entity.getFirstName(),
                        entity.getLastName(),
                        new Email(entity.getEmail()),
                        entity.getExternalOrganizationId(),
                        entity.getExternalUnitId(),
                        entity.getRole(),
                        entity.isEmailVerified(),
                        entity.isActive(),
                        entity.isMustChangePassword()));
    }
}