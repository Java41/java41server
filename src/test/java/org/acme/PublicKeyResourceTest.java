package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class PublicKeyResourceTest {

    @Test
    public void testGetPublicKeySuccess() {
        given()
                .when()
                .get("/public-key")
                .then()
                .statusCode(200)
                .body(startsWith("-----BEGIN PUBLIC KEY-----"));
    }

    // Тест для случая отсутствия publicKey.pem потребует мока или изменения конфигурации
}