package org.acme.controller;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.dto.AuthDTO;
import org.acme.dto.TokenResponseDTO;
import org.acme.model.RefreshToken;
import org.acme.model.User;
import org.acme.util.TokenUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Path("/auth")
@RequestScoped
@Tag(name = "Authentication", description = "Endpoints for user authentication and authorization")
public class AuthResource {

    @Inject
    TokenUtils tokenUtils;

    @POST
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "Аутентификация пользователя", description = "Позволяет пользователю войти в систему, предоставив email и пароль. Возвращает JWT и refresh-токены.")
    @APIResponse(responseCode = "200", description = "Успешная аутентификация", content = @Content(schema = @Schema(implementation = TokenResponseDTO.class), example = "{\"accessToken\": \"eyJhbG...\", \"refreshToken\": \"f47ac10b-58cc-...\"}"))
    @APIResponse(responseCode = "400", description = "Отсутствуют обязательные поля или неверный формат", content = @Content(example = "{\"error\": \"Email, пароль и дата рождения обязательны\"}"))
    @APIResponse(responseCode = "401", description = "Неверные учетные данные", content = @Content(example = "{\"error\": \"Неверные учетные данные\"}"))
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

        String accessToken = tokenUtils.generateAccessToken(user);
        String refreshToken = tokenUtils.generateRefreshToken(user);

        return Response.ok(new TokenResponseDTO(accessToken, refreshToken)).build();
    }

    @POST
    @Path("/register")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "Регистрация пользователя", description = "Регистрирует нового пользователя и возвращает JWT и refresh-токены")
    @APIResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован", content = @Content(schema = @Schema(implementation = TokenResponseDTO.class), example = "{\"accessToken\": \"eyJhbG...\", \"refreshToken\": \"a1b2c3d4-e5f6-...\"}"))
    @APIResponse(responseCode = "400", description = "Отсутствуют обязательные поля", content = @Content(example = "{\"error\": \"Email, пароль и дата рождения обязательны\"}"))
    @APIResponse(responseCode = "409", description = "Email уже существует", content = @Content(example = "{\"error\": \"Пользователь с таким email уже существует\"}"))
    public Response register(@RequestBody(description = "Данные для регистрации") AuthDTO.Registration registration) {
        if (registration.email == null || registration.password == null || registration.birthdate == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Email, пароль и дата рождения обязательны\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        // Валидация формата даты рождения
        try {
            LocalDate.parse(registration.birthdate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Дата рождения должна быть в формате YYYY-MM-DD\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Валидация длины имени и фамилии
        if (registration.firstName != null && registration.firstName.length() > 50) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Имя не должно превышать 50 символов\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (registration.lastName != null && registration.lastName.length() > 50) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Фамилия не должна превышать 50 символов\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (User.findByEmail(registration.email) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Пользователь с таким email уже существует\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        User.add(registration.email, registration.password, "User", registration.birthdate, registration.firstName, registration.lastName, null);
        User newUser = User.findByEmail(registration.email);

        String accessToken = tokenUtils.generateAccessToken(newUser);
        String refreshToken = tokenUtils.generateRefreshToken(newUser);

        return Response.status(Response.Status.CREATED)
                .entity(new TokenResponseDTO(accessToken, refreshToken))
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
    @APIResponse(responseCode = "200", description = "Выданы новые токены", content = @Content(schema = @Schema(implementation = TokenResponseDTO.class), example = "{\"accessToken\": \"eyJhbG...\", \"refreshToken\": \"f47ac10b-58cc-...\"}"))
    @APIResponse(responseCode = "400", description = "Отсутствует refresh токен", content = @Content(example = "{\"error\": \"Refresh token обязателен\"}"))
    @APIResponse(responseCode = "401", description = "Недействительный или истекший refresh токен", content = @Content(example = "{\"error\": \"Недействительный или истекший refresh token\"}"))
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

        User user = User.findById(refreshTokenEntity.userId);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Пользователь, связанный с refresh token, не найден\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        refreshTokenEntity.delete();
        String accessToken = tokenUtils.generateAccessToken(user);
        String newRefreshToken = tokenUtils.generateRefreshToken(user);

        return Response.ok(new TokenResponseDTO(accessToken, newRefreshToken)).build();
    }

    @POST
    @Path("/logout")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "Выход пользователя", description = "Аннулирует refresh токен")
    @APIResponse(responseCode = "200", description = "Успешный выход", content = @Content(example = "{\"message\": \"Logged out successfully\"}"))
    @APIResponse(responseCode = "400", description = "Отсутствует или недействительный refresh токен", content = @Content(example = "{\"error\": \"Invalid refresh token\"}"))
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
}