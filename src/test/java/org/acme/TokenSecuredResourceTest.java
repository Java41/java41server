package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.time.Instant;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
public class TokenSecuredResourceTest {

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
        String token = generateToken("jdoe@quarkus.io", "User", "2001-07-13");

        Response response = given()
                .auth().oauth2(token)
                .when()
                .get("/secured/roles-allowed")
                .andReturn();

        response.then()
                .statusCode(200)
                .body(containsString(
                        "hello + jdoe@quarkus.io, isHttps: false, authScheme: Bearer, hasJWT: true, birthdate: 2001-07-13"));
    }

    @Test
    public void testHelloRolesAllowedAdminOnlyWithUserRole() {
        String token = generateToken("jdoe@quarkus.io", "User", "2001-07-13");

        Response response = given()
                .auth().oauth2(token)
                .when()
                .get("/secured/roles-allowed-admin")
                .andReturn();

        response.then().statusCode(403);
    }

    @Test
    public void testHelloRolesAllowedAdmin() {
        String token = generateToken("admin@quarkus.io", "Admin", "1990-01-15");

        Response response = given()
                .auth().oauth2(token)
                .when()
                .get("/secured/roles-allowed")
                .andReturn();

        response.then()
                .statusCode(200)
                .body(containsString(
                        "hello + admin@quarkus.io, isHttps: false, authScheme: Bearer, hasJWT: true, birthdate: 1990-01-15"));
    }

    @Test
    public void testHelloRolesAllowedAdminOnlyWithAdminRole() {
        String token = generateToken("admin@quarkus.io", "Admin", "1990-01-15");

        Response response = given()
                .auth().oauth2(token)
                .when()
                .get("/secured/roles-allowed-admin")
                .andReturn();

        response.then()
                .statusCode(200)
                .body(containsString(
                        "hello + admin@quarkus.io, isHttps: false, authScheme: Bearer, hasJWT: true, birthdate: 1990-01-15"));
    }

    @Test
    public void testHelloRolesAllowedExpiredToken() {
        String token = generateExpiredToken("jdoe@quarkus.io", "User", "2001-07-13");

        Response response = given()
                .auth().oauth2(token)
                .when()
                .get("/secured/roles-allowed")
                .andReturn();

        response.then().statusCode(401);
    }

    @Test
    public void testHelloDenyAll() {
        String token = generateToken("jdoe@quarkus.io", "User", "2001-07-13");

        Response response = given()
                .auth().oauth2(token)
                .when()
                .get("/secured/deny-all")
                .andReturn();

        response.then().statusCode(403);
    }

    private String generateToken(String upn, String group, String birthdate) {
        return Jwt.issuer("https://example.com/issuer")
                .upn(upn)
                .groups(group)
                .claim(Claims.birthdate.name(), birthdate)
                .sign();
    }

    private String generateExpiredToken(String upn, String group, String birthdate) {
        return Jwt.issuer("https://example.com/issuer")
                .upn(upn)
                .groups(group)
                .claim(Claims.birthdate.name(), birthdate)
                .expiresAt(Instant.now().minusSeconds(3600))
                .sign();
    }
}