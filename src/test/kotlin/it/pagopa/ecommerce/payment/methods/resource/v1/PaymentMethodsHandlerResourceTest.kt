package it.pagopa.ecommerce.payment.methods.resource.v1

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.TestUtils
import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.v1.server.model.FeeRange
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson
import it.pagopa.generated.ecommerce.client.model.FeeRangeDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import jakarta.validation.ValidationException
import jakarta.ws.rs.core.Response
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@QuarkusTest
class PaymentMethodsHandlerResourceTest {
    @InjectMock lateinit var mockClient: PaymentMethodsClient
    private val request: PaymentMethodsRequest = TestUtils.buildDefaultMockRequest()

    @Test
    fun shouldReturnOKResponse() {
        val mockResponseDto =
            PaymentMethodsResponseDto()
                .addPaymentMethodsItem(
                    PaymentMethodsItemDto()
                        .paymentMethodId("test-id")
                        .status(PaymentMethodsItemDto.StatusEnum.ENABLED)
                        .group(PaymentMethodsItemDto.GroupEnum.CP)
                        .methodManagement(PaymentMethodsItemDto.MethodManagementEnum.ONBOARDABLE)
                        .feeRange(FeeRangeDto().min(1).max(10))
                        .name(mapOf(Pair("IT", "Carte")))
                        .description(mapOf(Pair("IT", "Carte")))
                        .paymentMethodAsset("asset")
                        .paymentMethodsBrandAssets(mapOf(Pair("first", "asset")))
                        .metadata(mapOf("test" to "test"))
                )
        val expectedBody =
            PaymentMethodsResponse()
                .addPaymentMethodsItem(
                    PaymentMethodResponse()
                        .id("test-id")
                        .status(PaymentMethodResponse.StatusEnum.ENABLED)
                        .paymentTypeCode(PaymentMethodResponse.PaymentTypeCodeEnum.CP)
                        .methodManagement(PaymentMethodResponse.MethodManagementEnum.ONBOARDABLE)
                        .feeRange(FeeRange().min(1).max(10))
                        .name(mapOf("IT" to "Carte"))
                        .description(mapOf("IT" to "Carte"))
                        .paymentMethodAsset("asset")
                        .paymentMethodsBrandAssets(mapOf("first" to "asset"))
                        .metadata(mapOf("test" to "test"))
                )
        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull())).then {
            Uni.createFrom().item { mockResponseDto }
        }

        val result =
            RestAssured.given()
                .header("x-api-key", "test-primary")
                .contentType(ContentType.JSON)
                .body(request)
                .`when`()
                .post("/payment-methods")
                .then()
                .statusCode(200)
                .extract()
                .`as`(PaymentMethodsResponse::class.java)

        assertEquals(expectedBody, result)
    }

    @Test
    fun shouldHandlePaymentMethodsClientException() {
        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull()))
            .thenReturn(Uni.createFrom().failure(PaymentMethodsClientException("test")))

        val expectedProblem = ProblemJson()
        expectedProblem.status = Response.Status.INTERNAL_SERVER_ERROR.statusCode
        expectedProblem.title = "Unexpected Exception"
        expectedProblem.detail = "Error during GMP communication"

        val result =
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("x-api-key", "test-primary")
                .body(request)
                .`when`()
                .post("/payment-methods")
                .then()
                .statusCode(500)
                .extract()
                .`as`(ProblemJson::class.java)

        assertEquals(expectedProblem, result)
    }

    @Test
    fun shouldHandleValidationException() {
        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull()))
            .thenReturn(Uni.createFrom().failure(ValidationException("test")))

        val expectedProblem = ProblemJson()
        expectedProblem.status = Response.Status.BAD_REQUEST.statusCode
        expectedProblem.title = "Bad Request"
        expectedProblem.detail =
            "The request is malformed, contains invalid parameters, or is missing required information."

        val result =
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("x-api-key", "test-primary")
                .body(request)
                .`when`()
                .post("/payment-methods")
                .then()
                .statusCode(400)
                .extract()
                .`as`(ProblemJson::class.java)

        assertEquals(expectedProblem, result)
    }

    @Test
    fun shouldHandleException() {
        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull()))
            .thenReturn(Uni.createFrom().failure(Exception("test")))

        val expectedProblem = ProblemJson()
        expectedProblem.status = Response.Status.INTERNAL_SERVER_ERROR.statusCode
        expectedProblem.title = "Unexpected Exception"
        expectedProblem.detail = "Generic Error"

        val result =
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("x-api-key", "test-primary")
                .body(request)
                .`when`()
                .post("/payment-methods")
                .then()
                .statusCode(500)
                .extract()
                .`as`(ProblemJson::class.java)

        assertEquals(expectedProblem, result)
    }
}
