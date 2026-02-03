package com.ivan.backend.application.port;

import com.ivan.backend.application.dto.LoginRequest;
import com.ivan.backend.application.dto.LoginResponse;

public interface LoginInputPort {
    // On utilise les objets de transport de l'application
    LoginResponse login(LoginRequest request);
}
