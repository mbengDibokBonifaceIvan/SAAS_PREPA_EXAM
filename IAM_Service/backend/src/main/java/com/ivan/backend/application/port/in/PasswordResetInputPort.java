package com.ivan.backend.application.port.in;


public interface PasswordResetInputPort {
    void requestReset(String email);
}