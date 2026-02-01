package com.ivan.backend.domain.repository;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.Email;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(Email email);
}