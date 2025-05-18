package org.acme;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class AuthDTO {

    @Schema(description = "Login credentials")
    public static class Login {
        @Schema(
                description = "User's email",
                required = true,
                examples = {"user@example.com", "admin@domain.com"}
        )
        public String email;

        @Schema(
                description = "User's password",
                required = true,
                examples = {"password123", "securePass!2023"}
        )
        public String password;
    }

    @Schema(description = "Registration details")
    public static class Registration {
        @Schema(
                description = "User's email",
                required = true,
                examples = {"new.user@example.com", "registrant@mail.org"}
        )
        public String email;

        @Schema(
                description = "User's password",
                required = true,
                examples = {"Str0ngP@ss", "Qwerty!123"}
        )
        public String password;

        @Schema(
                description = "User's birthdate (YYYY-MM-DD)",
                required = true,
                pattern = "^\\d{4}-\\d{2}-\\d{2}$",
                examples = {"1990-01-01", "2000-12-31"}
        )
        public String birthdate;
    }

    @Schema(description = "Refresh token")
    public static class Refresh {
        @Schema(
                description = "Refresh token",
                required = true,
                pattern = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89aAbB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$",
                examples = {"f47ac10b-58cc-4372-a567-0e02b2c3d479", "550e8400-e29b-41d4-a716-446655440000"}
        )
        public String refreshToken;
    }

    @Schema(description = "Update email request")
    public static class UpdateEmailDTO {
        @Schema(
                description = "New email address",
                required = true,
                format = "email",
                examples = {"new.email@example.com", "updated@mail.org"}
        )
        public String email;

        @Schema(
                description = "Current password",
                required = true,
                examples = {"currentPassword123", "oldSecure!Pass"}
        )
        public String password;
    }

    @Schema(description = "Update username request")
    public static class UpdateUsernameDTO {
        @Schema(
                description = "New username starting with @",
                required = true,
                pattern = "^@[a-zA-Z0-9_]{3,20}$",
                examples = {"@NewUser_123", "@UpdatedUsername"}
        )
        public String username;
    }
}