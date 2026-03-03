package com.ivan.backend.application.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorTest {

    @Test
    @DisplayName("Génération : devrait avoir une longueur de 12 caractères")
    void shouldHaveCorrectLength() {
        String password = PasswordGenerator.generate();
        assertEquals(12, password.length());
    }

    @RepeatedTest(10)
    @DisplayName("Génération : devrait respecter toutes les contraintes de complexité")
    void shouldMeetComplexityRequirements() {
        String password = PasswordGenerator.generate();

        assertTrue(password.matches(".*[A-Z].*"), "Doit contenir au moins une majuscule");
        assertTrue(password.matches(".*[a-z].*"), "Doit contenir au moins une minuscule");
        assertTrue(password.matches(".*[0-9].*"), "Doit contenir au moins un chiffre");
        assertTrue(password.matches(".*[!@#$%^&*()\\-_+=<>?].*"), "Doit contenir au moins un caractère spécial");
    }

    @Test
    @DisplayName("Aléatoire : deux mots de passe successifs devraient être différents")
    void shouldBeRandom() {
        String pass1 = PasswordGenerator.generate();
        String pass2 = PasswordGenerator.generate();
        assertNotEquals(pass1, pass2, "Les mots de passe générés ne devraient pas être identiques");
    }
}