package org.acme.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Информация о контакте")
public class ContactDTO {
    @Schema(description = "Идентификатор пользователя", hidden = true)
    public Long id;

    @Schema(description = "Имя пользователя, начинающееся с @", hidden = true)
    public String username;

    @Schema(description = "Имя пользователя", hidden = true, nullable = true)
    public String firstName;

    @Schema(description = "Фамилия пользователя", hidden = true, nullable = true)
    public String lastName;

    @Schema(description = "URL фотографии пользователя", hidden = true, nullable = true)
    public String photoUrl;

    public ContactDTO(Long id, String username, String firstName, String lastName, String photoUrl) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.photoUrl = photoUrl;
    }
}