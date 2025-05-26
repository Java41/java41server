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
public class AuthResourceTest {

    @BeforeEach
    @Transactional
    public void setup() {
        // Очищаем только связанные таблицы
        Contact.deleteAll();
        Message.deleteAll();
        RefreshToken.deleteAll();
    }

    // --- Registration Tests ---
    @Test
    public void testRegisterSuccess() {
        AuthDTO.Registration registration = new AuthDTO.Registration();
        registration.email = "testuser-" + UUID.randomUUID() + "@quarkus.io";
        registration.password = "password123";
        registration.birthdate = "2001-07-13";
        registration.firstName = "John";
        registration.lastName = "Doe";

        given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    public void testRegisterDuplicateEmail() {
        String email = "duplicate-" + UUID.randomUUID() + "@quarkus.io";
        AuthDTO.Registration registration = new AuthDTO.Registration();
        registration.email = email;
        registration.password = "password123";
        registration.birthdate = "2001-07-13";
        registration.firstName = "John";
        registration.lastName = "Doe";

        given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(409)
                .body("error", is("Пользователь с таким email уже существует"));
    }

    @Test
    public void testRegisterMissingFields() {
        AuthDTO.Registration registration = new AuthDTO.Registration();
        registration.email = null;
        registration.password = null;
        registration.birthdate = null;

        given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400)
                .body("error", is("Email, пароль и дата рождения обязательны"));
    }

    @Test
    public void testRegisterInvalidBirthdate() {
        AuthDTO.Registration registration = new AuthDTO.Registration();
        registration.email = "testuser-" + UUID.randomUUID() + "@quarkus.io";
        registration.password = "password123";
        registration.birthdate = "invalid-date";
        registration.firstName = "John";
        registration.lastName = "Doe";

        given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400)
                .body("error", is("Дата рождения должна быть в формате YYYY-MM-DD"));
    }

    @Test
    public void testRegisterLongFirstName() {
        AuthDTO.Registration registration = new AuthDTO.Registration();
        registration.email = "testuser-" + UUID.randomUUID() + "@quarkus.io";
        registration.password = "password123";
        registration.birthdate = "2001-07-13";
        registration.firstName = "A".repeat(51);
        registration.lastName = "Doe";

        given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400)
                .body("error", is("Имя не должно превышать 50 символов"));
    }

    // --- Login Tests ---
    @Test
    public void testLoginSuccess() {
        String email = "loginuser-" + UUID.randomUUID() + "@quarkus.io";
        String password = "password123";

        AuthDTO.Registration registration = new AuthDTO.Registration();
        registration.email = email;
        registration.password = password;
        registration.birthdate = "2001-07-13";
        registration.firstName = "John";
        registration.lastName = "Doe";

        given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        AuthDTO.Login login = new AuthDTO.Login();
        login.email = email;
        login.password = password;

        given()
                .contentType(ContentType.JSON)
                .body(login)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    public void testLoginInvalidCredentials() {
        AuthDTO.Login login = new AuthDTO.Login();
        login.email = "nonexistent-" + UUID.randomUUID() + "@quarkus.io";
        login.password = "wrongpassword";

        given()
                .contentType(ContentType.JSON)
                .body(login)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .body("error", is("Неверные учетные данные"));
    }

    @Test
    public void testLoginMissingFields() {
        AuthDTO.Login login = new AuthDTO.Login();
        login.email = null;
        login.password = null;

        given()
                .contentType(ContentType.JSON)
                .body(login)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("error", is("Email и пароль обязательны"));
    }

    // --- Refresh Token Tests ---
    @Test
    public void testRefreshTokenSuccess() {
        String email = "refreshuser-" + UUID.randomUUID() + "@quarkus.io";
        String password = "password123";

        AuthDTO.Registration registration = new AuthDTO.Registration();
        registration.email = email;
        registration.password = password;
        registration.birthdate = "2001-07-13";
        registration.firstName = "John";
        registration.lastName = "Doe";

        Response regResponse = given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .andReturn();

        String refreshToken = regResponse.jsonPath().getString("refreshToken");

        AuthDTO.Refresh refresh = new AuthDTO.Refresh();
        refresh.refreshToken = refreshToken;

        given()
                .contentType(ContentType.JSON)
                .body(refresh)
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    public void testRefreshTokenInvalid() {
        AuthDTO.Refresh refresh = new AuthDTO.Refresh();
        refresh.refreshToken = "invalid-token-" + UUID.randomUUID();

        given()
                .contentType(ContentType.JSON)
                .body(refresh)
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(401)
                .body("error", is("Недействительный или истекший refresh token"));
    }

    @Test
    public void testRefreshTokenMissing() {
        AuthDTO.Refresh refresh = new AuthDTO.Refresh();
        refresh.refreshToken = null;

        given()
                .contentType(ContentType.JSON)
                .body(refresh)
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(400)
                .body("error", is("Refresh token обязателен"));
    }

    // --- Logout Tests ---
    @Test
    public void testLogoutSuccess() {
        String email = "logoutuser-" + UUID.randomUUID() + "@quarkus.io";
        String password = "password123";

        AuthDTO.Registration registration = new AuthDTO.Registration();
        registration.email = email;
        registration.password = password;
        registration.birthdate = "2001-07-13";
        registration.firstName = "John";
        registration.lastName = "Doe";

        Response regResponse = given()
                .contentType(ContentType.JSON)
                .body(registration)
                .when()
                .post("/auth/register")
                .andReturn();

        String refreshToken = regResponse.jsonPath().getString("refreshToken");

        AuthDTO.Refresh refresh = new AuthDTO.Refresh();
        refresh.refreshToken = refreshToken;

        given()
                .contentType(ContentType.JSON)
                .body(refresh)
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(200)
                .body("message", is("Logged out successfully"));
    }

    @Test
    public void testLogoutInvalidToken() {
        AuthDTO.Refresh refresh = new AuthDTO.Refresh();
        refresh.refreshToken = "invalid-token-" + UUID.randomUUID();

        given()
                .contentType(ContentType.JSON)
                .body(refresh)
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(400)
                .body("error", is("Invalid refresh token"));
    }

    @Test
    public void testLogoutMissingToken() {
        AuthDTO.Refresh refresh = new AuthDTO.Refresh();
        refresh.refreshToken = null;

        given()
                .contentType(ContentType.JSON)
                .body(refresh)
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(400)
                .body("error", is("Refresh token обязателен для выхода"));
    }
}