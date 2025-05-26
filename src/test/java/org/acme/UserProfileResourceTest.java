package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.transaction.Transactional;
import org.acme.controller.UserProfileResource;
import org.acme.dto.AuthDTO;
import org.acme.model.Contact;
import org.acme.model.Message;
import org.acme.model.RefreshToken;
import org.acme.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class UserProfileResourceTest {

    private String accessToken;
    private Long userId;

    @BeforeEach
    @Transactional
    public void setup() {
        // Очищаем только связанные таблицы
        Contact.deleteAll();
        Message.deleteAll();
        RefreshToken.deleteAll();

        String email = "testuser-" + UUID.randomUUID() + "@quarkus.io";
        AuthDTO.Registration registration = new AuthDTO.Registration();
        registration.email = email;
        registration.password = "password123";
        registration.birthdate = "2001-07-13";
        registration.firstName = "John";
        registration.lastName = "Doe";

        Response response = given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .andReturn();

        accessToken = response.jsonPath().getString("accessToken");
        userId = User.findByEmail(email).id;
    }

    @Test
    public void testGetUserProfileSuccess() {
        given()
                .auth().oauth2(accessToken)
                .when()
                .get("/profile/" + userId)
                .then()
                .statusCode(200)
                .body("id", is(userId.intValue()))
                .body("username", startsWith("@User"))
                .body("firstName", is("John"))
                .body("lastName", is("Doe"));
    }

    @Test
    public void testGetUserProfileNotFound() {
        given()
                .auth().oauth2(accessToken)
                .when()
                .get("/profile/999")
                .then()
                .statusCode(404)
                .body("error", is("Пользователь не найден"));
    }

    @Test
    public void testPatchProfileSuccess() {
        UserProfileResource.PatchProfileDTO patch = new UserProfileResource.PatchProfileDTO();
        patch.firstName = "Jane";

        Response patchResponse = given()
                .auth().oauth2(accessToken)
                .contentType(ContentType.JSON)
                .body(patch)
                .when()
                .patch("/profile")
                .andReturn();

        // Логируем ответ для отладки
        System.out.println("Patch Response: " + patchResponse.asString());

        // Проверяем, что PATCH вернул токены
        patchResponse.then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());

        // Проверяем обновление firstName через GET /profile/{userId}
        String newAccessToken = patchResponse.jsonPath().getString("accessToken");
        given()
                .auth().oauth2(newAccessToken)
                .when()
                .get("/profile/" + userId)
                .then()
                .statusCode(200)
                .body("firstName", is("Jane"));
    }

    @Test
    public void testPatchProfileEmailWithPassword() {
        UserProfileResource.PatchProfileDTO patch = new UserProfileResource.PatchProfileDTO();
        patch.email = "newemail-" + UUID.randomUUID() + "@quarkus.io";
        patch.password = "password123";

        given()
                .auth().oauth2(accessToken)
                .contentType(ContentType.JSON)
                .body(patch)
                .when()
                .patch("/profile")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    public void testPatchProfileEmailWithoutPassword() {
        UserProfileResource.PatchProfileDTO patch = new UserProfileResource.PatchProfileDTO();
        patch.email = "newemail-" + UUID.randomUUID() + "@quarkus.io";

        given()
                .auth().oauth2(accessToken)
                .contentType(ContentType.JSON)
                .body(patch)
                .when()
                .patch("/profile")
                .then()
                .statusCode(400)
                .body("error", is("Пароль обязателен для обновления email"));
    }

    @Test
    public void testPatchProfileDuplicateEmail() {
        String email2 = "user2-" + UUID.randomUUID() + "@quarkus.io";
        AuthDTO.Registration registration = new AuthDTO.Registration();
        registration.email = email2;
        registration.password = "password123";
        registration.birthdate = "2001-07-13";
        registration.firstName = "Jane";
        registration.lastName = "Smith";

        given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        UserProfileResource.PatchProfileDTO patch = new UserProfileResource.PatchProfileDTO();
        patch.email = email2;
        patch.password = "password123";

        given()
                .auth().oauth2(accessToken)
                .contentType(ContentType.JSON)
                .body(patch)
                .when()
                .patch("/profile")
                .then()
                .statusCode(409)
                .body("error", is("Пользователь с таким email уже существует"));
    }
}