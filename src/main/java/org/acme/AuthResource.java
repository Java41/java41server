package org.acme;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

@Path("/auth")
@RequestScoped
public class AuthResource {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @POST
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response login(AuthDTO.Login login) {
        if (login.email == null || login.password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Email и пароль обязательны\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        User user = User.findByEmail(login.email);
        if (user == null || !BcryptUtil.matches(login.password, user.password)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Неверные учетные данные\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        return Response.ok(new TokenResponse(accessToken, refreshToken)).build();
    }

    @POST
    @Path("/register")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response register(AuthDTO.Registration registration) {
        if (registration.email == null || registration.password == null || registration.birthdate == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Email, пароль и дата рождения обязательны\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (User.findByEmail(registration.email) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Пользователь с таким email уже существует\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        User.add(registration.email, registration.password, "User", registration.birthdate);

        return Response.status(Response.Status.CREATED)
                .entity("{\"message\":\"Пользователь успешно зарегистрирован\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    @POST
    @Path("/refresh")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response refreshToken(AuthDTO.Refresh refresh) {
        if (refresh.refreshToken == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Refresh token обязателен\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        RefreshToken refreshTokenEntity = RefreshToken.findByToken(refresh.refreshToken);
        if (refreshTokenEntity == null || refreshTokenEntity.expiryDate.isBefore(LocalDateTime.now())) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Недействительный или истекший refresh token\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        User user = User.findByEmail(refreshTokenEntity.email);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Пользователь, связанный с refresh token, не найден\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        refreshTokenEntity.delete();
        String accessToken = generateAccessToken(user);
        String newRefreshToken = generateRefreshToken(user);

        return Response.ok(new TokenResponse(accessToken, newRefreshToken)).build();
    }

    @POST
    @Path("/logout")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response logout(AuthDTO.Refresh refresh) {
        if (refresh.refreshToken == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Refresh token обязателен для выхода\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        RefreshToken refreshTokenEntity = RefreshToken.findByToken(refresh.refreshToken);
        if (refreshTokenEntity != null) {
            refreshTokenEntity.delete();
            return Response.ok().entity("{\"message\":\"Logged out successfully\"}").type(MediaType.APPLICATION_JSON).build();
        }
        return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Invalid refresh token\"}").type(MediaType.APPLICATION_JSON).build();
    }

    private String generateAccessToken(User user) {
        return Jwt.issuer(issuer)
                .upn(user.email)
                .groups(new HashSet<>(Arrays.asList(user.roles.split(","))))
                .claim(Claims.birthdate.name(), user.birthdate)
                .expiresIn(900) // 15 минут
                .sign();
    }

    private String generateRefreshToken(User user) {
        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.token = refreshTokenValue;
        refreshToken.email = user.email;
        refreshToken.expiryDate = LocalDateTime.now().plusDays(7);
        refreshToken.persist();
        return refreshTokenValue;
    }
}
