package com.ivan.backend.infrastructure.adapter.persistence;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository repository;

    @Override
    public User save(User user) {
        UserEntity entity = new UserEntity();
        entity.setUserId(user.getId());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setEmail(user.getEmail().value());
        entity.setRole(user.getRole());
        entity.setExternalOrganizationId(user.getTenantId());
        
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
                        entity.getRole()
                ));
    }
}