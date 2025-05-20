package org.acme.dto;


import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Информация о контакте")
public class ContactDTO {
    @Schema(description = "Идентификатор пользователя",
            examples = {"1", "2", "3"},
            defaultValue = "1")
    public Long id;

    @Schema(description = "Имя пользователя, начинающееся с @",
            examples = {"@User123", "@AnotherUser"},
            defaultValue = "@User123")
    public String username;

    public ContactDTO(Long id, String username) {
        this.id = id;
        this.username = username;
    }
}