package org.acme.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class AddContactDTO {
    @Schema(description = "ID пользователя для добавления в контакты", required = true)
    public Long id;
}
