package org.acme.controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.acme.dto.MessageDTO;
import org.acme.model.Message;
import org.acme.model.User;
import org.acme.repository.MessageRepository;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


@Path("/messages")
@Tag(name = "Messages", description = "Operations related to messages")
public class MessageResource {

    @Inject
    MessageRepository messageRepository;

    @Inject
    SecurityContext securityContext;

    @POST
    @Path("/")
    @RolesAllowed("User")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(
            summary = "Отправка сообщения",
            description = "Отправляет сообщение от текущего пользователя указанному получателю. Требуется JWT-токен в заголовке Authorization."
    )
    @APIResponse(
            responseCode = "201",
            description = "Сообщение успешно отправлено",
            content = @Content(
                    schema = @Schema(implementation = MessageDTO.MessageResponse.class),
                    example = "{\"id\": 123, \"senderId\": 456, \"senderUsername\": \"@Sender\", \"recipientId\": 789, \"recipientUsername\": \"@Recipient\", \"content\": \"Привет!\", \"timestamp\": \"2024-01-26T14:30:00\"}"
            )
    )
    @APIResponse(responseCode = "400", description = "Неверные данные запроса", content = @Content(
            example = "{\"error\": \"Не указан ID получателя или текст сообщения\"}"
    ))
    @APIResponse(responseCode = "404", description = "Получатель не найден", content = @Content(
            example = "{\"error\": \"Получатель не найден\"}"
    ))
    public Response sendMessage(
            @RequestBody(
                    description = "Данные сообщения",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = MessageDTO.CreateMessage.class)
                    )
            ) MessageDTO.CreateMessage messageData,
            @Context SecurityContext securityContext) {

        if (messageData.recipientId == null || messageData.content == null || messageData.content.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Recipient ID and message content are required\"}")
                    .build();
        }

        String senderEmail = securityContext.getUserPrincipal().getName();
        User sender = User.findByEmail(senderEmail);
        User recipient = User.findById(messageData.recipientId);

        if (recipient == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Recipient not found\"}")
                    .build();
        }

        Message message = messageRepository.create(sender, recipient, messageData.content);

        MessageDTO.MessageResponse response = new MessageDTO.MessageResponse(
                message.id,
                sender.id,
                sender.username,
                recipient.id,
                recipient.username,
                message.content,
                message.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/")
    @RolesAllowed("User")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Получение сообщений",
            description = "Получает сообщения для текущего пользователя. Можно фильтровать по переписке с конкретным пользователем или с определенной временной метки.")
    @APIResponse(
            responseCode = "200",
            description = "Список сообщений",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(
                            implementation = MessageDTO.MessageResponse.class,
                            type = SchemaType.ARRAY
                    ),
                    example = "[{\"id\": 123, \"senderId\": 456, \"senderUsername\": \"@Sender\", \"recipientId\": 789, \"recipientUsername\": \"@Recipient\", \"content\": \"Привет!\", \"timestamp\": \"2024-01-26T14:30:00\"}, {\"id\": 456, \"senderId\": 789, \"senderUsername\": \"@Recipient\", \"recipientId\": 456, \"recipientUsername\": \"@Sender\", \"content\": \"Привет в ответ!\", \"timestamp\": \"2024-01-26T14:35:00\"}]"
            )
    )
    @APIResponse(responseCode = "400", description = "Неверный формат параметра 'since'", content = @Content(
            example = "{\"error\": \"Неверный формат параметра 'since', используйте ISO 8601 (например, 2025-05-19T10:00:00)\"}"
    ))
    @APIResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(
            example = "{\"error\": \"Пользователь не найден\"}"
    ))
    public Response getMessages(
            @QueryParam("with") Long withUserId,
            @QueryParam("since") @DefaultValue("1970-01-01T00:00:00") String since,
            @Context SecurityContext securityContext) {

        String currentUserEmail = securityContext.getUserPrincipal().getName();
        User currentUser = User.findByEmail(currentUserEmail);

        LocalDateTime sinceTimestamp;
        try {
            sinceTimestamp = LocalDateTime.parse(since, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invalid 'since' format, use ISO 8601 (e.g., 2025-05-19T10:00:00)\"}")
                    .build();
        }

        List<Message> messages;
        if (withUserId != null) {
            User otherUser = User.findById(withUserId);
            if (otherUser == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"User not found\"}")
                        .build();
            }
            messages = messageRepository.findConversationSince(currentUser.id, otherUser.id, sinceTimestamp);
        } else {
            messages = messageRepository.findByParticipantSince(currentUser.id, sinceTimestamp);
        }

        List<MessageDTO.MessageResponse> response = messages.stream()
                .map(m -> new MessageDTO.MessageResponse(
                        m.id,
                        m.sender.id,
                        m.sender.username,
                        m.recipient.id,
                        m.recipient.username,
                        m.content,
                        m.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ))
                .collect(Collectors.toList());

        return Response.ok(response).build();
    }
}