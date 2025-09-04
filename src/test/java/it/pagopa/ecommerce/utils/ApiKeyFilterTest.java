package it.pagopa.ecommerce.utils;



import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class ApiKeyFilterTest {

    private static final String SECURED_PATH = "/hello";

    @Test
    public void testWithoutApiKeyShouldReturnUnauthorized() {
        RestAssured
                .given()
                .when()
                .get(SECURED_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    public void testWithValidPrimaryApiKeyShouldReturnOk() {
        RestAssured
                .given()
                .header("x-api-key", "test-primary")
                .when()
                .get(SECURED_PATH)
                .then()
                .statusCode(200);
    }

    @Test
    public void testWithInvalidApiKeyShouldReturnUnauthorized() {
        RestAssured
                .given()
                .header("x-api-key", "invalid-key")
                .when()
                .get(SECURED_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    public void testWithValidSecondaryApiKeyShouldReturnOk() {
        RestAssured
                .given()
                .header("x-api-key", "test-secondary")
                .when()
                .get(SECURED_PATH)
                .then()
                .statusCode(200)
                .body(equalTo("Hello RESTEasy"));
    }
}
