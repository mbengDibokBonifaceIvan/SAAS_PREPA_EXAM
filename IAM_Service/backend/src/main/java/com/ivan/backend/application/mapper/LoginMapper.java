package com.ivan.backend.application.mapper;

import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.AuthToken;

public class LoginMapper {

    private LoginMapper(){}
    
    public static LoginResponse toResponse(User user, AuthToken token) {
        return new LoginResponse(
            token.accessToken(),
            token.refreshToken(),
            token.expiresIn(),
            user.getEmail().value(),
            user.isMustChangePassword(),
            user.getRole().name()
        );
    }
}