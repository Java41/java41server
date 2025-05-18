package org.acme;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.*;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


@Path("/messages")
@Tag(name = "Messages", description = "Operations related to messages")
public class MessageResource {

    @Inject
    SecurityContext securityContext;

    @POST
    @Path("/")
    @RolesAllowed("User")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "Send a new message",
            description = "Creates and sends a new message to another user")
    @APIResponse(
            responseCode = "201",
            description = "Message successfully sent",
            content = @Content(
                    schema = @Schema(implementation = MessageDTO.MessageResponse.class)
            )
    )
    @APIResponse(responseCode = "400", description = "Invalid request data")
    @APIResponse(responseCode = "404", description = "Recipient not found")
    public Response sendMessage(
            @RequestBody(
                    description = "Message data",
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

        Message message = Message.create(sender, recipient, messageData.content);

        MessageDTO.MessageResponse response = new MessageDTO.MessageResponse(
                message.id,
                sender.id,
                recipient.id,
                message.content,
                message.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/")
    @RolesAllowed("User")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get messages",
            description = "Get messages for the current user. Can filter by conversation with specific user.")
    @APIResponse(
            responseCode = "200",
            description = "List of messages",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(
                            implementation = MessageDTO.MessageResponse.class,
                            type = SchemaType.ARRAY
                    )
            )
    )
    public Response getMessages(
            @Parameter(
                    description = "Filter conversation by user ID",
                    schema = @Schema(implementation = Long.class)
            )
            @QueryParam("with") Long withUserId,
            @Context SecurityContext securityContext) {

        String currentUserEmail = securityContext.getUserPrincipal().getName();
        User currentUser = User.findByEmail(currentUserEmail);

        List<Message> messages;
        if (withUserId != null) {
            User otherUser = User.findById(withUserId);
            if (otherUser == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"User not found\"}")
                        .build();
            }
            messages = Message.findConversation(currentUser.id, otherUser.id);
        } else {
            messages = Message.findByParticipant(currentUser.id);
        }

        List<MessageDTO.MessageResponse> response = messages.stream()
                .map(m -> new MessageDTO.MessageResponse(
                        m.id,
                        m.sender.id,
                        m.recipient.id,
                        m.content,
                        m.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ))
                .collect(Collectors.toList());

        return Response.ok(response).build();
    }
}