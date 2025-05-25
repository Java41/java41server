package org.acme.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Информация о профиле пользователя")
public class UserProfileDTO {

    @Schema(description = "Идентификатор пользователя", examples = "1")
    public Long id;

    @Schema(description = "Имя пользователя, начинающееся с @", examples = "@User123")
    public String username;

    @Schema(description = "Электронная почта пользователя", examples = "user@example.com")
    public String email;

    @Schema(description = "Дата рождения пользователя (YYYY-MM-DD)", examples = "1990-01-01")
    public String birthdate;

    @Schema(description = "Имя пользователя", examples = "John", nullable = true)
    public String firstName;

    @Schema(description = "Фамилия пользователя", examples = "Doe", nullable = true)
    public String lastName;

    @Schema(description = "URL фотографии пользователя", examples = "https://example.com/photo.jpg", nullable = true)
    public String photoUrl;

    public UserProfileDTO(Long id, String username, String email, String birthdate, String firstName, String lastName, String photoUrl) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.birthdate = birthdate;
        this.firstName = firstName;
        this.lastName = lastName;
        this.photoUrl = photoUrl;
    }
}