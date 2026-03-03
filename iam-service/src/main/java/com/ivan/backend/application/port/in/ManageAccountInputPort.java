package com.ivan.backend.application.port.in;

public interface ManageAccountInputPort {
    void banAccount(String email, String ownerEmail);
    void activateAccount(String email, String ownerEmail);
}
