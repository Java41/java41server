package org.acme.controller;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.acme.dto.TokenResponseDTO;
import org.acme.dto.UserProfileDTO;
import org.acme.model.User;
import org.acme.util.TokenUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/profile")
@Tag(name = "Профиль пользователя", description = "Операции для управления профилем пользователя")
public class UserProfileResource {

    @Inject
    TokenUtils tokenUtils;

    @GET
    @Path("/{userId}")
    @RolesAllowed("User")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Получить профиль пользователя по ID", description = "Возвращает информацию о профиле пользователя с указанным ID.")
    @APIResponse(responseCode = "200", description = "Профиль пользователя", content = @Content(schema = @Schema(implementation = UserProfileDTO.class), example = "{\"id\": 1, \"username\": \"@User123\", \"email\": \"user@example.com\", \"birthdate\": \"1990-01-01\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"photoUrl\": \"https://example.com/photo.jpg\"}"))
    @APIResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(example = "{\"error\": \"Пользователь не найден\"}"))
    public Response getUserProfile(@PathParam("userId") Long userId) {
        User user = User.findById(userId);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Пользователь не найден\"}")
                    .build();
        }

        UserProfileDTO profile = new UserProfileDTO(user.id, user.username, user.email, user.birthdate, user.firstName, user.lastName, user.photoUrl);
        return Response.ok(profile).build();
    }

    @PATCH
    @RolesAllowed("User")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "Частичное обновление профиля", description = "Обновляет указанные поля профиля пользователя. Для email требуется пароль. Возвращает новые токены при обновлении email, username, firstName или lastName, иначе возвращает обновлённый профиль.")
    @APIResponse(responseCode = "200", description = "Профиль или токены успешно обновлены", content = {
            @Content(schema = @Schema(implementation = UserProfileDTO.class), example = "{\"id\": 1, \"username\": \"@User123\", \"email\": \"user@example.com\", \"birthdate\": \"1990-01-01\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"photoUrl\": \"https://example.com/photo.jpg\"}", mediaType = MediaType.APPLICATION_JSON),
            @Content(schema = @Schema(implementation = TokenResponseDTO.class), example = "{\"accessToken\": \"eyJhbG...\", \"refreshToken\": \"f47ac10b-58cc-...\"}", mediaType = MediaType.APPLICATION_JSON)
    })
    @APIResponse(responseCode = "400", description = "Неверные данные", content = @Content(example = "{\"error\": \"Неверные данные профиля\"}"))
    @APIResponse(responseCode = "401", description = "Не авторизован или неверный пароль", content = @Content(example = "{\"error\": \"Неверные учетные данные\"}"))
    @APIResponse(responseCode = "409", description = "Email или username уже занят", content = @Content(example = "{\"error\": \"Пользователь с таким email уже существует\"}"))
    public Response patchProfile(@Context SecurityContext ctx, @RequestBody(description = "Поля профиля для обновления") PatchProfileDTO patchProfileDTO) {
        String currentUserId = ctx.getUserPrincipal().getName(); // Получаем ID из sub
        if (currentUserId == null || currentUserId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Невалидный JWT-токен: отсутствует ID пользователя\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        Long userId;
        try {
            userId = Long.parseLong(currentUserId);
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Невалидный ID пользователя в токене\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        User user = User.findById(userId);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Пользователь не найден\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        boolean requiresTokenUpdate = false;

        // Валидация и обновление email
        if (patchProfileDTO.email != null) {
            if (patchProfileDTO.password == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Пароль обязателен для обновления email\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            if (!BcryptUtil.matches(patchProfileDTO.password, user.password)) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Неверный пароль\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            if (User.findByEmail(patchProfileDTO.email) != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\": \"Пользователь с таким email уже существует\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            user.email = patchProfileDTO.email;
            requiresTokenUpdate = true;
        }

        // Валидация и обновление username
        if (patchProfileDTO.username != null) {
            if (!patchProfileDTO.username.startsWith("@") || patchProfileDTO.username.length() < 4 || patchProfileDTO.username.length() > 21) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Username обязателен, должен начинаться с @ и содержать от 3 до 20 символов\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            if (User.findByUsername(patchProfileDTO.username) != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\": \"Username уже занят\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            user.username = patchProfileDTO.username;
            requiresTokenUpdate = true;
        }

        // Валидация и обновление firstName
        if (patchProfileDTO.firstName != null) {
            if (patchProfileDTO.firstName.length() > 50) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Имя не должно превышать 50 символов\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            user.firstName = patchProfileDTO.firstName;
            requiresTokenUpdate = true;
        }

        // Валидация и обновление lastName
        if (patchProfileDTO.lastName != null) {
            if (patchProfileDTO.lastName.length() > 50) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Фамилия не должна превышать 50 символов\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            user.lastName = patchProfileDTO.lastName;
            requiresTokenUpdate = true;
        }

        // Валидация и обновление photoUrl
        if (patchProfileDTO.photoUrl != null) {
            if (patchProfileDTO.photoUrl.length() > 255) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"URL фотографии не должен превышать 255 символов\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            user.photoUrl = patchProfileDTO.photoUrl;
        }

        // Проверка, что хотя бы одно поле указано
        if (patchProfileDTO.email == null && patchProfileDTO.username == null &&
                patchProfileDTO.firstName == null && patchProfileDTO.lastName == null &&
                patchProfileDTO.photoUrl == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Укажите хотя бы одно поле для обновления\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        user.persist();

        // Возвращаем токены, если обновлялись email, username, firstName или lastName
        if (requiresTokenUpdate) {
            String accessToken = tokenUtils.generateAccessToken(user);
            String refreshToken = tokenUtils.generateRefreshToken(user);
            return Response.ok(new TokenResponseDTO(accessToken, refreshToken)).build();
        }

        // Иначе возвращаем обновлённый профиль
        UserProfileDTO profile = new UserProfileDTO(user.id, user.username, user.email, user.birthdate, user.firstName, user.lastName, user.photoUrl);
        return Response.ok(profile).build();
    }

    public static class PatchProfileDTO {
        @Schema(description = "Новый email", examples = "new.email@example.com", nullable = true)
        public String email;

        @Schema(description = "Пароль (обязателен для обновления email)", examples = "password123", nullable = true)
        public String password;

        @Schema(description = "Новое имя пользователя, начинающееся с @", examples = "@NewUser123", nullable = true)
        public String username;

        @Schema(description = "Имя пользователя", examples = "John", nullable = true, maxLength = 50)
        public String firstName;

        @Schema(description = "Фамилия пользователя", examples = "Doe", nullable = true, maxLength = 50)
        public String lastName;

        @Schema(description = "URL фотографии пользователя", examples = "https://example.com/photo.jpg", nullable = true, maxLength = 255)
        public String photoUrl;
    }
}