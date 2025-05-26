package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.transaction.Transactional;
import org.acme.dto.AuthDTO;
import org.acme.model.Contact;
import org.acme.model.Message;
import org.acme.model.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class UserResourceTest {

    private String accessToken;

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
    }

    @Test
    public void testGetAllUsersSuccess() {
        given()
                .auth().oauth2(accessToken)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].username", startsWith("@User"))
                .body("[0].firstName", is("John"))
                .body("[0].lastName", is("Doe"));
    }

    @Test
    public void testGetAllUsersUnauthorized() {
        given()
                .when()
                .get("/users")
                .then()
                .statusCode(401);
    }
}