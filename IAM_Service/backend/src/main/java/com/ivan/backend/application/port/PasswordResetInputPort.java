package com.ivan.backend.application.port;


public interface PasswordResetInputPort {
    void requestReset(String email);
}