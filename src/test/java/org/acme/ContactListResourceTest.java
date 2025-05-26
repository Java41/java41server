package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.transaction.Transactional;
import org.acme.dto.AddContactDTO;
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
public class ContactListResourceTest {

    private String accessToken1;
    private Long userId2;

    @BeforeEach
    @Transactional
    public void setup() {
        // Очищаем только связанные таблицы
        Contact.deleteAll();
        Message.deleteAll();
        RefreshToken.deleteAll();

        // Регистрируем первого пользователя
        String email1 = "user1-" + UUID.randomUUID() + "@quarkus.io";
        AuthDTO.Registration reg1 = new AuthDTO.Registration();
        reg1.email = email1;
        reg1.password = "password123";
        reg1.birthdate = "2001-07-13";
        reg1.firstName = "John";
        reg1.lastName = "Doe";

        Response response1 = given()
                .contentType(ContentType.JSON)
                .body(reg1)
                .when()
                .post("/auth/register")
                .andReturn();

        accessToken1 = response1.jsonPath().getString("accessToken");

        // Регистрируем второго пользователя
        String email2 = "user2-" + UUID.randomUUID() + "@quarkus.io";
        AuthDTO.Registration reg2 = new AuthDTO.Registration();
        reg2.email = email2;
        reg2.password = "password123";
        reg2.birthdate = "2001-07-13";
        reg2.firstName = "Jane";
        reg2.lastName = "Smith";

        Response response2 = given()
                .contentType(ContentType.JSON)
                .body(reg2)
                .when()
                .post("/auth/register")
                .andReturn();

        userId2 = User.findByEmail(email2).id;
    }

    @Test
    public void testAddContactSuccess() {
        AddContactDTO contactDTO = new AddContactDTO();
        contactDTO.id = userId2;

        given()
                .auth().oauth2(accessToken1)
                .contentType(ContentType.JSON)
                .body(contactDTO)
                .when()
                .post("/contacts")
                .then()
                .statusCode(201)
                .body("id", is(userId2.intValue()))
                .body("firstName", is("Jane"))
                .body("lastName", is("Smith"));
    }

    @Test
    public void testAddContactSelf() {
        String email1 = "user1-" + UUID.randomUUID() + "@quarkus.io";
        AuthDTO.Registration reg1 = new AuthDTO.Registration();
        reg1.email = email1;
        reg1.password = "password123";
        reg1.birthdate = "2001-07-13";
        reg1.firstName = "John";
        reg1.lastName = "Doe";

        Response response1 = given()
                .contentType(ContentType.JSON)
                .body(reg1)
                .when()
                .post("/auth/register")
                .andReturn();

        String accessToken = response1.jsonPath().getString("accessToken");
        Long userId1 = User.findByEmail(email1).id;

        AddContactDTO contactDTO = new AddContactDTO();
        contactDTO.id = userId1;

        given()
                .auth().oauth2(accessToken)
                .contentType(ContentType.JSON)
                .body(contactDTO)
                .when()
                .post("/contacts")
                .then()
                .statusCode(400)
                .body("error", is("ID пользователя обязателен и не должен быть равен ID текущего пользователя"));
    }

    @Test
    public void testGetContactsSuccess() {
        AddContactDTO contactDTO = new AddContactDTO();
        contactDTO.id = userId2;

        given()
                .auth().oauth2(accessToken1)
                .contentType(ContentType.JSON)
                .body(contactDTO)
                .when()
                .post("/contacts")
                .then()
                .statusCode(201);

        given()
                .auth().oauth2(accessToken1)
                .when()
                .get("/contacts")
                .then()
                .statusCode(200)
                .body("[0].id", is(userId2.intValue()))
                .body("[0].firstName", is("Jane"));
    }
}