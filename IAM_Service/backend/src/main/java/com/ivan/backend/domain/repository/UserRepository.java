package com.ivan.backend.domain.repository;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(Email email);
    Optional<User> findByRoleAndTenantId(UserRole role, UUID tenantId);
}