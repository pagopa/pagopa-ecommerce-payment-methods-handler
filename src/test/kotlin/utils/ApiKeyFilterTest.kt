package utils

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.Test

@QuarkusTest
class ApiKeyFilterTest {

    private val securedPath = "/payment-methods-handler"

    @Test
    fun testWithoutApiKeyShouldReturnUnauthorized() {
        RestAssured.given()
            .header("x-client-id", "test")
            .`when`()
            .get("$securedPath/payment-methods")
            .then()
            .statusCode(401)
    }

    @Test
    fun testWithValidPrimaryApiKeyShouldReturnOk() {
        RestAssured.given()
            .header("x-client-id", "test")
            .header("x-api-key", "test-primary")
            .queryParam("amount", "100")
            .`when`()
            .get("$securedPath/payment-methods")
            .then()
            .statusCode(200)
    }

    @Test
    fun testWithInvalidApiKeyShouldReturnUnauthorized() {
        RestAssured.given()
            .header("x-client-id", "test")
            .header("x-api-key", "invalid-key")
            .`when`()
            .get("$securedPath/payment-methods")
            .then()
            .statusCode(401)
    }

    @Test
    fun testWithValidSecondaryApiKeyShouldReturnOk() {
        RestAssured.given()
            .header("x-client-id", "test")
            .header("x-api-key", "test-secondary")
            .queryParam("amount", "100")
            .`when`()
            .get("$securedPath/payment-methods")
            .then()
            .statusCode(200)
    }
}
