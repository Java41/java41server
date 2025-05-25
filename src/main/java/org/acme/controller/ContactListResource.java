package org.acme.controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.acme.dto.AddContactDTO;
import org.acme.dto.ContactDTO;
import org.acme.model.Contact;
import org.acme.model.User;
import org.acme.repository.ContactRepository;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.stream.Collectors;

@Path("/contacts")
@Tag(name = "Контакты", description = "Операции для управления списком контактов пользователя")
public class ContactListResource {

    @Inject
    ContactRepository contactRepository;

    @Inject
    SecurityContext securityContext;

    @POST
    @RolesAllowed("User")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "Добавить пользователя в контакты", description = "Добавляет указанного пользователя в список контактов текущего пользователя.")
    @APIResponse(responseCode = "201", description = "Контакт успешно добавлен", content = @Content(schema = @Schema(implementation = ContactDTO.class), example = "{\"id\": 2, \"username\": \"@User456\", \"firstName\": \"Jane\", \"lastName\": \"Smith\", \"photoUrl\": \"https://example.com/photo2.jpg\"}"))
    @APIResponse(responseCode = "400", description = "Неверный ID пользователя или попытка добавить себя", content = @Content(example = "{\"error\": \"ID пользователя обязателен и не должен быть равен ID текущего пользователя\"}"))
    @APIResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(example = "{\"error\": \"Пользователь не найден\"}"))
    @APIResponse(responseCode = "409", description = "Пользователь уже в контактах", content = @Content(example = "{\"error\": \"Пользователь уже в списке контактов\"}"))
    public Response addContact(@RequestBody(description = "ID пользователя для добавления в контакты") AddContactDTO contactDTO) {
        String currentUserId = securityContext.getUserPrincipal().getName();
        User currentUser = User.findById(Long.parseLong(currentUserId));

        if (contactDTO.id == null || contactDTO.id.equals(currentUser.id)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"ID пользователя обязателен и не должен быть равен ID текущего пользователя\"}")
                    .build();
        }

        User contactUser = User.findById(contactDTO.id);
        if (contactUser == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Пользователь не найден\"}")
                    .build();
        }

        if (contactRepository.exists(currentUser.id, contactUser.id)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Пользователь уже в списке контактов\"}")
                    .build();
        }

        Contact contact = new Contact();
        contact.owner = currentUser;
        contact.contact = contactUser;
        contact.persist();

        return Response.status(Response.Status.CREATED)
                .entity(new ContactDTO(contactUser.id, contactUser.username, contactUser.firstName, contactUser.lastName, contactUser.photoUrl))
                .build();
    }

    @GET
    @RolesAllowed("User")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Получить список контактов", description = "Возвращает список контактов текущего пользователя.")
    @APIResponse(responseCode = "200", description = "Список контактов", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContactDTO.class, type = SchemaType.ARRAY), example = "[{\"id\": 2, \"username\": \"@User456\", \"firstName\": \"Jane\", \"lastName\": \"Smith\", \"photoUrl\": \"https://example.com/photo2.jpg\"}]"))
    public List<ContactDTO> getContacts() {
        String currentUserEmail = securityContext.getUserPrincipal().getName();
        User currentUser = User.findByEmail(currentUserEmail);

        return contactRepository.findByOwner(currentUser.id)
                .stream()
                .map(contact -> new ContactDTO(contact.contact.id, contact.contact.username, contact.contact.firstName, contact.contact.lastName, contact.contact.photoUrl))
                .collect(Collectors.toList());
    }
}