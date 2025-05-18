package org.acme;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class AuthDTO {

    @Schema(description = "Login credentials")
    public static class Login {
        @Schema(description = "User's email", required = true, example = "user@example.com")
        public String email;
        @Schema(description = "User's password", required = true, example = "password123")
        public String password;
    }

    @Schema(description = "Registration details")
    public static class Registration {
        @Schema(description = "User's email", required = true, example = "user@example.com")
        public String email;
        @Schema(description = "User's password", required = true, example = "password123")
        public String password;
        @Schema(description = "User's birthdate (YYYY-MM-DD)", required = true, example = "1990-01-01")
        public String birthdate;
    }

    @Schema(description = "Refresh token")
    public static class Refresh {
        @Schema(description = "Refresh token", required = true, example = "uuid-refresh-token")
        public String refreshToken;
    }

    @Schema(description = "Update email request")
    public static class UpdateEmailDTO {
        @Schema(description = "New email address", required = true, example = "newemail@example.com")
        public String email;
        @Schema(description = "Current password", required = true, example = "password123")
        public String password;
    }

    @Schema(description = "Update username request")
    public static class UpdateUsernameDTO {
        @Schema(description = "New username starting with @", required = true, example = "@NewUser123")
        public String username;
    }
}