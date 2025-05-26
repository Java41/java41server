package org.acme;

import io.smallrye.jwt.build.Jwt;
import org.acme.model.User;
import org.acme.util.TokenUtils;
import jakarta.inject.Inject;

public class GenerateToken {

    @Inject
    TokenUtils tokenUtils;

    public static void main(String[] args) {
        User user = new User();
        user.id = 1L;
        user.email = "jdoe@quarkus.io";
        user.username = "@User123";
        user.roles = "User";
        user.birthdate = "2001-07-13";
        user.firstName = "John";
        user.lastName = "Doe";

        TokenUtils tokenUtils = new TokenUtils();
        String token = tokenUtils.generateAccessToken(user);
        System.out.println(token);
        System.exit(0);
    }
}