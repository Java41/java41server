package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.time.Instant;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;

/**
 * Tests of the TokenSecuredResource REST endpoints
 */
@QuarkusTest
public class TokenSecuredResourceTest {

    // Ключи для подписи токенов в тестах должны соответствовать тем,
    // что используются приложением (publicKey.pem для проверки).
    // SmallRye JWT Build API по умолчанию использует ключи из classpath,
    // если они названы jwt-sign.key и jwt-verify.key или настроены через microprofile-config.properties
    // Для простоты здесь токены генерируются без явного указания ключей,
    // полагаясь на то, что smallrye-jwt-build их найдет или использует временные.
    // Для более точного соответствия production-конфигурации, можно загружать privateKey.pem
    // и использовать его для подписи.

    @Test
    public void testHelloEndpoint() {
        Response response = given()
                .when()
                .get("/secured/permit-all")
                .andReturn();

        response.then()
                .statusCode(200)
                .body(containsString("hello + anonymous, isHttps: false, authScheme: null, hasJWT: false"));
    }

    @Test
    public void testHelloRolesAllowedUser() {
        Response response = given().auth()
                .oauth2(generateToken("jdoe@quarkus.io", "User", "2001-07-13"))
                .when()
                .get("/secured/roles-allowed").andReturn();

        response.then()
                .statusCode(200)
                .body(containsString(
                        "hello + jdoe@quarkus.io, isHttps: false, authScheme: Bearer, hasJWT: true, birthdate: 2001-07-13"));
    }

    @Test
    public void testHelloRolesAllowedAdminOnlyWithUserRole() {
        Response response = given().auth()
                .oauth2(generateToken("jdoe@quarkus.io", "User", "2001-07-13"))
                .when()
                .get("/secured/roles-allowed-admin").andReturn();

        response.then().statusCode(403);
    }

    @Test
    public void testHelloRolesAllowedAdmin() {
        Response response = given().auth()
                .oauth2(generateToken("admin@quarkus.io", "Admin", "1990-01-15"))
                .when()
                .get("/secured/roles-allowed").andReturn();

        response.then()
                .statusCode(200)
                .body(containsString(
                        "hello + admin@quarkus.io, isHttps: false, authScheme: Bearer, hasJWT: true, birthdate: 1990-01-15"));
    }

    @Test
    public void testHelloRolesAllowedAdminOnlyWithAdminRole() {
        Response response = given().auth()
                .oauth2(generateToken("admin@quarkus.io", "Admin", "1990-01-15"))
                .when()
                .get("/secured/roles-allowed-admin").andReturn();

        response.then()
                .statusCode(200)
                .body(containsString(
                        "hello + admin@quarkus.io, isHttps: false, authScheme: Bearer, hasJWT: true, birthdate: 1990-01-15"));
    }

    @Test
    public void testHelloRolesAllowedExpiredToken() {
        Response response = given().auth()
                .oauth2(generateExpiredToken("jdoe@quarkus.io", "User", "2001-07-13"))
                .when()
                .get("/secured/roles-allowed").andReturn();

        response.then().statusCode(401);
    }

    @Test
    public void testHelloRolesAllowedModifiedToken() {
        Response response = given().auth()
                .oauth2(generateToken("jdoe@quarkus.io", "User", "2001-07-13") + "1")
                .when()
                .get("/secured/roles-allowed").andReturn();

        response.then().statusCode(401);
    }

    @Test
    public void testHelloRolesAllowedWrongIssuer() {
        Response response = given().auth()
                .oauth2(generateWrongIssuerToken("jdoe@quarkus.io", "User", "2001-07-13"))
                .when()
                .get("/secured/roles-allowed").andReturn();

        response.then().statusCode(401);
    }

    @Test
    public void testHelloDenyAll() {
        Response response = given().auth()
                .oauth2(generateToken("jdoe@quarkus.io", "User", "2001-07-13"))
                .when()
                .get("/secured/deny-all").andReturn();

        response.then().statusCode(403);
    }

    // Helper methods for token generation
    // Чтобы эти токены работали с вашим приложением, они должны быть подписаны
    // тем же privateKey.pem, который используется сервером для smallrye.jwt.sign.key.location
    // и проверяться соответствующим publicKey.pem.
    // Если ключи не указаны явно, smallrye-jwt-build может использовать дефолтные или временные ключи.
    // Для полной уверенности, особенно если тесты падают на валидации токена,
    // следует настроить генерацию токенов с использованием ваших ключей.

    private String generateToken(String upn, String group, String birthdate) {
        return Jwt.upn(upn)
                .issuer("https://example.com/issuer") // Должен совпадать с mp.jwt.verify.issuer
                .groups(group)
                .claim(Claims.birthdate.name(), birthdate)
                .sign(); // Предполагается, что SmallRye найдет privateKey.pem в resources
    }

    private String generateExpiredToken(String upn, String group, String birthdate) {
        return Jwt.upn(upn)
                .issuer("https://example.com/issuer")
                .groups(group)
                .claim(Claims.birthdate.name(), birthdate)
                .expiresAt(Instant.now().minusSeconds(3600)) // Истек час назад
                .sign();
    }

    private String generateWrongIssuerToken(String upn, String group, String birthdate) {
        return Jwt.upn(upn)
                .issuer("https://wrong.example.com/issuer") // Неверный issuer
                .groups(group)
                .claim(Claims.birthdate.name(), birthdate)
                .sign();
    }
}
