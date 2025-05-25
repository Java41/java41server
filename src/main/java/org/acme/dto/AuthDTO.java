package org.acme.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class AuthDTO {

    @Schema(description = "Login credentials")
    public static class Login {
        @Schema(description = "User's email", required = true, examples = {"user@example.com"}, defaultValue = "user@example.com")
        public String email;

        @Schema(description = "User's password", required = true, examples = {"password123"}, defaultValue = "password123", minLength = 8, maxLength = 64)
        public String password;
    }

    @Schema(description = "Registration details")
    public static class Registration {
        @Schema(description = "User's email", required = true, examples = {"user@example.com"}, defaultValue = "user@example.com")
        public String email;

        @Schema(description = "User's password", required = true, examples = {"password123"}, defaultValue = "password123", minLength = 8, maxLength = 64)
        public String password;

        @Schema(description = "User's birthdate (YYYY-MM-DD)", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", examples = {"1990-01-01"}, defaultValue = "password123")
        public String birthdate;

        @Schema(description = "User's first name", required = true, examples = {"Aleksandr"}, defaultValue = "Aleksandr", maxLength = 50)
        public String firstName;

        @Schema(description = "User's last name", required = true, examples = {"Borodavkin"}, defaultValue = "Borodavkin", maxLength = 50)
        public String lastName;
    }

    @Schema(description = "Refresh token")
    public static class Refresh {
        @Schema(description = "Refresh token", required = true, pattern = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89aAbB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$", examples = {"f47ac10b-58cc-4372-a567-0e02b2c3d479"}, defaultValue = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        public String refreshToken;
    }

}