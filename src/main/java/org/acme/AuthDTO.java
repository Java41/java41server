package org.acme;

public class AuthDTO {

    public static class Login {
        public String email;
        public String password;
    }

    public static class Registration {
        public String email;
        public String password;
        public String birthdate;
    }

    public static class Refresh {
        public String refreshToken;
    }
}