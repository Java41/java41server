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

import java.time.Instant;
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
    @Operation(
            summary = "Аутентификация пользователя",
            description = "Позволяет пользователю войти в систему, предоставив email и пароль. Возвращает JWT и refresh-токены для дальнейшего доступа к защищенным ресурсам."
    )
    @APIResponse(
            responseCode = "200",
            description = "Успешная аутентификация",
            content = @Content(
                    schema = @Schema(implementation = AuthResource.UserInfo.class),
                    example = "{\"id\": 1, \"username\": \"@User123\", \"email\": \"user@example.com\", \"accessToken\": \"eyJhbG...\", \"refreshToken\": \"f47ac10b-58cc-...\"}"
            )
    )
    @APIResponse(
            responseCode = "400",
            description = "Отсутствует email или пароль",
            content = @Content(
                    example = "{\"error\": \"Email и пароль обязательны\"}"
            )
    )
    @APIResponse(
            responseCode = "401",
            description = "Неверные учетные данные",
            content = @Content(
                    example = "{\"error\": \"Неверные учетные данные\"}"
            )
    )
    public Response login(@RequestBody(description = "Учетные данные для входа") AuthDTO.Login login) {
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
    @Operation(summary = "Регистрация пользователя", description = "Регистрирует нового пользователя и возвращает JWT и refresh токены")
    @APIResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован", content = @Content(schema = @Schema(implementation = UserInfo.class),
            example = "{\"id\": 2, \"username\": \"@NewUser\", \"email\": \"newuser@example.com\", \"accessToken\": \"eyJhbG...\", \"refreshToken\": \"a1b2c3d4-e5f6-...\"}"
    ))
    @APIResponse(responseCode = "400", description = "Отсутствуют обязательные поля", content = @Content(
            example = "{\"error\": \"Email, пароль и дата рождения обязательны\"}"
    ))
    @APIResponse(responseCode = "409", description = "Email уже существует", content = @Content(
            example = "{\"error\": \"Пользователь с таким email уже существует\"}"
    ))
    public Response register(@RequestBody(description = "Данные для регистрации") AuthDTO.Registration registration) {
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
    @Operation(summary = "Обновление токена", description = "Обновляет JWT с использованием действительного refresh токена")
    @APIResponse(responseCode = "200", description = "Выданы новые токены", content = @Content(schema = @Schema(implementation = UserInfo.class),
            example = "{\"id\": 1, \"username\": \"@User123\", \"email\": \"user@example.com\", \"accessToken\": \"eyJhbG...\", \"refreshToken\": \"f47ac10b-58cc-...\"}"
    ))
    @APIResponse(responseCode = "400", description = "Отсутствует refresh токен", content = @Content(
            example = "{\"error\": \"Refresh token обязателен\"}"
    ))
    @APIResponse(responseCode = "401", description = "Недействительный или истекший refresh токен", content = @Content(
            example = "{\"error\": \"Недействительный или истекший refresh token\"}"
    ))
    public Response refreshToken(@RequestBody(description = "Refresh токен") AuthDTO.Refresh refresh) {
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
    @Operation(summary = "Выход пользователя", description = "Аннулирует refresh токен")
    @APIResponse(responseCode = "200", description = "Успешный выход", content = @Content(
            example = "{\"message\": \"Logged out successfully\"}"
    ))
    @APIResponse(responseCode = "400", description = "Отсутствует или недействительный refresh токен", content = @Content(
            example = "{\"error\": \"Invalid refresh token\"}"  // Или "Refresh token обязателен для выхода", в зависимости от логики
    ))
    public Response logout(@RequestBody(description = "Refresh токен") AuthDTO.Refresh refresh) {
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
    @Operation(summary = "Обновление email пользователя", description = "Обновляет адрес электронной почты пользователя")
    @APIResponse(responseCode = "200", description = "Email успешно обновлен", content = @Content(schema = @Schema(implementation = UserInfo.class),
            example = "{\"id\": 1, \"username\": \"@User123\", \"email\": \"newemail@example.com\", \"accessToken\": \"eyJhbG...\", \"refreshToken\": \"f47ac10b-58cc-...\"}"
    ))
    @APIResponse(responseCode = "400", description = "Отсутствует email или пароль", content = @Content(
            example = "{\"error\": \"Email и пароль обязательны\"}"
    ))
    @APIResponse(responseCode = "401", description = "Неверные учетные данные", content = @Content(
            example = "{\"error\": \"Неверные учетные данные\"}"
    ))
    @APIResponse(responseCode = "409", description = "Email уже существует", content = @Content(
            example = "{\"error\": \"Пользователь с таким email уже существует\"}"
    ))
    public Response updateEmail(@Context SecurityContext ctx, @RequestBody(description = "Новый email и пароль") AuthDTO.UpdateEmailDTO updateEmailDTO) {
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
    @Operation(summary = "Обновление имени пользователя", description = "Обновляет имя пользователя")
    @APIResponse(responseCode = "200", description = "Имя пользователя успешно обновлено", content = @Content(schema = @Schema(implementation = UserInfo.class),
            example = "{\"id\": 1, \"username\": \"@NewUsername\", \"email\": \"user@example.com\", \"accessToken\": \"eyJhbG...\", \"refreshToken\": \"f47ac10b-58cc-...\"}"
    ))
    @APIResponse(responseCode = "400", description = "Отсутствует или недействительное имя пользователя", content = @Content(
            example = "{\"error\": \"Username обязателен и должен начинаться с @\"}"
    ))
    @APIResponse(responseCode = "401", description = "Не авторизован", content = @Content(
            example = "{\"error\": \"Пользователь не найден\"}"  // Или "Неверные учетные данные", в зависимости от логики
    ))
    @APIResponse(responseCode = "409", description = "Имя пользователя уже занято", content = @Content(
            example = "{\"error\": \"Username уже занят\"}"
    ))
    public Response updateUsername(@Context SecurityContext ctx, @RequestBody(description = "Новое имя пользователя", required = true) AuthDTO.UpdateUsernameDTO updateUsernameDTO) {
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
                .issuedAt(Instant.now())
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