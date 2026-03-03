package com.ivan.backend.application.util;

public class PasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()-_+=<>?";

    private static final String ALL_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_CHARACTERS;
    private static final int PASSWORD_LENGTH = 12;

    public static String generate() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);

        // Ensure the password has at least one character from each category
        password.append(getRandomCharacter(UPPERCASE));
        password.append(getRandomCharacter(LOWERCASE));
        password.append(getRandomCharacter(DIGITS));
        password.append(getRandomCharacter(SPECIAL_CHARACTERS));

        // Fill the remaining length with random characters from all categories
        for (int i = 4; i < PASSWORD_LENGTH; i++) {
            password.append(getRandomCharacter(ALL_CHARACTERS));
        }

        // Shuffle the characters to ensure randomness
        return shuffleString(password.toString());
    }

    private static char getRandomCharacter(String characters) {
        int index = (int) (Math.random() * characters.length());
        return characters.charAt(index);
    }

    private static String shuffleString(String input) {
        char[] characters = input.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            int randomIndex = (int) (Math.random() * characters.length);
            // Swap characters
            char temp = characters[i];
            characters[i] = characters[randomIndex];
            characters[randomIndex] = temp;
        }
        return new String(characters);
    }
    
}
