package org.acme.util;

import io.smallrye.jwt.build.Jwt;
import org.acme.model.RefreshToken;
import org.acme.model.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

@ApplicationScoped
public class TokenUtils {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    public String generateAccessToken(User user) {
        return Jwt.issuer(issuer)
                .subject(user.id.toString())
                .groups(new HashSet<>(Arrays.asList(user.roles.split(","))))
                .claim(Claims.birthdate.name(), user.birthdate)
                .claim("email", user.email)
                .claim("username", user.username)
                .claim("firstName", user.firstName)
                .claim("lastName", user.lastName)
                .issuedAt(Instant.now())
                .expiresIn(900)
                .sign();
    }

    public String generateRefreshToken(User user) {
        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.token = refreshTokenValue;
        refreshToken.userId = user.id;
        refreshToken.expiryDate = LocalDateTime.now().plusDays(7);
        refreshToken.persist();
        return refreshTokenValue;
    }
}