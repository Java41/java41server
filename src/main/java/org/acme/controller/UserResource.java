package org.acme.controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.dto.ContactDTO;
import org.acme.model.User;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.stream.Collectors;

@Path("/users")
@Tag(name = "Пользователи", description = "Операции для получения списка всех пользователей")
public class UserResource {

    @GET
    @RolesAllowed("User")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Получить список всех пользователей", description = "Возвращает список всех зарегистрированных пользователей для выбора контакта.")
    @APIResponse(responseCode = "200", description = "Список всех пользователей", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContactDTO.class, type = SchemaType.ARRAY), example = "[{\"id\": 1, \"username\": \"@User123\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"photoUrl\": \"https://example.com/photo.jpg\"}]"))
    public List<ContactDTO> getAllUsers() {
        return User.<User>findAll()
                .stream()
                .map(user -> new ContactDTO(user.id, user.username, user.firstName, user.lastName, user.photoUrl))
                .collect(Collectors.toList());
    }
}