package org.acme.controller;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Path("/public-key")
@Tag(name = "Публичный ключ", description = "Эндпоинт для получения публичного ключа для проверки JWT")
public class PublicKeyResource {

    @GET
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Получить публичный ключ", description = "Возвращает публичный ключ в формате PEM для проверки подписи JWT-токенов")
    @APIResponse(responseCode = "200", description = "Публичный ключ в формате PEM")
    @APIResponse(responseCode = "500", description = "Ошибка чтения ключа")
    public Response getPublicKey() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("publicKey.pem")) {
            if (is == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\":\"Публичный ключ не найден\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            String publicKey = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Response.ok(publicKey).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Ошибка чтения публичного ключа: " + e.getMessage() + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}