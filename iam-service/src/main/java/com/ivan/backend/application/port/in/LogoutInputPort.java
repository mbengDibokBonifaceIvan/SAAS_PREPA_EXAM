package com.ivan.backend.application.port.in;

public interface LogoutInputPort {
    void execute(String refreshToken);
}
