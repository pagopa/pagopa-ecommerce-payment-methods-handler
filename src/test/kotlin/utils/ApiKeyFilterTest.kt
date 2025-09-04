package utils

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class ApiKeyFilterTest {

    private val securedPath = "/hello"

    @Test
    fun testWithoutApiKeyShouldReturnUnauthorized() {
        RestAssured.given().`when`().get(securedPath).then().statusCode(401)
    }

    @Test
    fun testWithValidPrimaryApiKeyShouldReturnOk() {
        RestAssured.given()
            .header("x-api-key", "test-primary")
            .`when`()
            .get(securedPath)
            .then()
            .statusCode(200)
    }

    @Test
    fun testWithInvalidApiKeyShouldReturnUnauthorized() {
        RestAssured.given()
            .header("x-api-key", "invalid-key")
            .`when`()
            .get(securedPath)
            .then()
            .statusCode(401)
    }

    @Test
    fun testWithValidSecondaryApiKeyShouldReturnOk() {
        RestAssured.given()
            .header("x-api-key", "test-secondary")
            .`when`()
            .get(securedPath)
            .then()
            .statusCode(200)
            .body(equalTo("Hello RESTEasy"))
    }
}
