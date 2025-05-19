package org.acme.controller;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.acme.model.RefreshToken;
import org.acme.dto.AuthDTO;
import org.acme.model.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Path("/auth")
@RequestScoped
@Tag(name = "Authentication", description = "Endpoints for user authentication and authorization")
public class AuthResource {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    public static class UserInfo {
        public Long id;
        public String username;
        public String email;
        public String accessToken;
        public String refreshToken;

        public UserInfo(Long id, String username, String email, String accessToken, String refreshToken) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    @POST
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "User login", description = "Authenticates a user and returns JWT and refresh tokens")
    @APIResponse(responseCode = "200", description = "Successful login", content = @Content(schema = @Schema(implementation = UserInfo.class)))
    @APIResponse(responseCode = "400", description = "Missing email or password")
    @APIResponse(responseCode = "401", description = "Invalid credentials")
    public Response login(@RequestBody(description = "Login credentials", required = true) AuthDTO.Login login) {
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

        return Response.ok(new UserInfo(user.id, user.username, user.email, accessToken, refreshToken)).build();
    }

    @POST
    @Path("/register")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "User registration", description = "Registers a new user and returns JWT and refresh tokens")
    @APIResponse(responseCode = "201", description = "User successfully registered", content = @Content(schema = @Schema(implementation = UserInfo.class)))
    @APIResponse(responseCode = "400", description = "Missing required fields")
    @APIResponse(responseCode = "409", description = "Email already exists")
    public Response register(@RequestBody(description = "Registration details", required = true) AuthDTO.Registration registration) {
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
        User newUser = User.findByEmail(registration.email);

        String accessToken = generateAccessToken(newUser);
        String refreshToken = generateRefreshToken(newUser);

        return Response.status(Response.Status.CREATED)
                .entity(new UserInfo(newUser.id, newUser.username, newUser.email, accessToken, refreshToken))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    @POST
    @Path("/refresh")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "Refresh token", description = "Refreshes JWT using a valid refresh token")
    @APIResponse(responseCode = "200", description = "New tokens issued", content = @Content(schema = @Schema(implementation = UserInfo.class)))
    @APIResponse(responseCode = "400", description = "Missing refresh token")
    @APIResponse(responseCode = "401", description = "Invalid or expired refresh token")
    public Response refreshToken(@RequestBody(description = "Refresh token", required = true) AuthDTO.Refresh refresh) {
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

        return Response.ok(new UserInfo(user.id, user.username, user.email, accessToken, newRefreshToken)).build();
    }

    @POST
    @Path("/logout")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "User logout", description = "Invalidates the refresh token")
    @APIResponse(responseCode = "200", description = "Successfully logged out")
    @APIResponse(responseCode = "400", description = "Missing or invalid refresh token")
    public Response logout(@RequestBody(description = "Refresh token", required = true) AuthDTO.Refresh refresh) {
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

    @POST
    @Path("/update-email")
    @RolesAllowed("User")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "Update user email", description = "Updates the user's email address")
    @APIResponse(responseCode = "200", description = "Email updated successfully", content = @Content(schema = @Schema(implementation = UserInfo.class)))
    @APIResponse(responseCode = "400", description = "Missing email or password")
    @APIResponse(responseCode = "401", description = "Invalid credentials")
    @APIResponse(responseCode = "409", description = "Email already exists")
    public Response updateEmail(@Context SecurityContext ctx, @RequestBody(description = "New email and password", required = true) AuthDTO.UpdateEmailDTO updateEmailDTO) {
        if (updateEmailDTO.email == null || updateEmailDTO.password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Email и пароль обязательны\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        String currentEmail = ctx.getUserPrincipal().getName();
        User user = User.findByEmail(currentEmail);
        if (user == null || !BcryptUtil.matches(updateEmailDTO.password, user.password)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Неверные учетные данные\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (User.findByEmail(updateEmailDTO.email) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Пользователь с таким email уже существует\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        user.email = updateEmailDTO.email;
        user.persist();

        List<RefreshToken> refreshTokens = RefreshToken.find("email", currentEmail).list();
        for (RefreshToken token : refreshTokens) {
            token.email = updateEmailDTO.email;
            token.persist();
        }

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        return Response.ok(new UserInfo(user.id, user.username, user.email, accessToken, refreshToken)).build();
    }

    @POST
    @Path("/update-username")
    @RolesAllowed("User")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "Update username", description = "Updates the user's username")
    @APIResponse(responseCode = "200", description = "Username updated successfully", content = @Content(schema = @Schema(implementation = UserInfo.class)))
    @APIResponse(responseCode = "400", description = "Missing or invalid username")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "409", description = "Username already taken")
    public Response updateUsername(@Context SecurityContext ctx, @RequestBody(description = "New username", required = true) AuthDTO.UpdateUsernameDTO updateUsernameDTO) {
        if (updateUsernameDTO.username == null || !updateUsernameDTO.username.startsWith("@")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username обязателен и должен начинаться с @\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        String currentEmail = ctx.getUserPrincipal().getName();
        User user = User.findByEmail(currentEmail);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Пользователь не найден\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (User.findByUsername(updateUsernameDTO.username) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Username уже занят\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        user.username = updateUsernameDTO.username;
        user.persist();

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        return Response.ok(new UserInfo(user.id, user.username, user.email, accessToken, refreshToken)).build();
    }

    private String generateAccessToken(User user) {
        return Jwt.issuer(issuer)
                .upn(user.email)
                .groups(new HashSet<>(Arrays.asList(user.roles.split(","))))
                .claim(Claims.birthdate.name(), user.birthdate)
                .claim("id", user.id)
                .claim("username", user.username)
                .expiresIn(900)
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