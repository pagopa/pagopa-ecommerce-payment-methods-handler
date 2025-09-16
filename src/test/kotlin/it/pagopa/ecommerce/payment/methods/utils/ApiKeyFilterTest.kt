package it.pagopa.ecommerce.payment.methods.utils

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.TestUtils
import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@QuarkusTest
class ApiKeyFilterTest {

    private val securedPath = "/payment-methods-handler"
    @InjectMock lateinit var mockClient: PaymentMethodsClient

    @Test
    fun testWithoutApiKeyShouldReturnUnauthorized() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .`when`()
            .post("$securedPath/payment-methods")
            .then()
            .statusCode(401)
    }

    @Test
    fun testWithValidPrimaryApiKeyShouldReturnOk() {
        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull())).then {
            Uni.createFrom().item { PaymentMethodsResponseDto() }
        }

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("x-api-key", "test-primary")
            .body(TestUtils.buildDefaultMockRequest())
            .`when`()
            .post("$securedPath/payment-methods")
            .then()
            .statusCode(200)
    }

    @Test
    fun testWithInvalidApiKeyShouldReturnUnauthorized() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("x-api-key", "invalid-key")
            .`when`()
            .post("$securedPath/payment-methods")
            .then()
            .statusCode(401)
    }

    @Test
    fun testWithValidSecondaryApiKeyShouldReturnOk() {
        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull())).then {
            Uni.createFrom().item { PaymentMethodsResponseDto() }
        }

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("x-api-key", "test-secondary")
            .body(TestUtils.buildDefaultMockRequest())
            .`when`()
            .post("$securedPath/payment-methods")
            .then()
            .statusCode(200)
    }
}
