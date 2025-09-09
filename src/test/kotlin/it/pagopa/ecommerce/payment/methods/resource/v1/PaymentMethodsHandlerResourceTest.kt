package it.pagopa.ecommerce.payment.methods.resource.v1

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodManagementType
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodStatus
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson
import it.pagopa.ecommerce.payment.methods.v1.server.model.Range
import it.pagopa.generated.ecommerce.client.model.FeeRangeDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import jakarta.ws.rs.core.Response
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@QuarkusTest
class PaymentMethodsHandlerResourceTest {
    private val securedPath = "/payment-methods-handler"
    @InjectMock lateinit var mockClient: PaymentMethodsClient

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
                        .name(mapOf(Pair("it", "Carte")))
                        .description(mapOf(Pair("it", "Carte")))
                        .paymentMethodAsset("asset")
                        .paymentMethodsBrandAssets(mapOf(Pair("first", "asset")))
                )
        val expectedBody =
            PaymentMethodsResponse()
                .addPaymentMethodsItem(
                    PaymentMethodResponse()
                        .id("test-id")
                        .status(PaymentMethodStatus.ENABLED)
                        .paymentTypeCode(PaymentMethodsItemDto.GroupEnum.CP.toString())
                        .methodManagement(PaymentMethodManagementType.ONBOARDABLE)
                        .ranges(listOf(Range().max(10).min(1)))
                        .name("Carte")
                        .description("Carte")
                        .asset("asset")
                        .brandAssets(mapOf(Pair("first", "asset")))
                )
        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull())).then {
            Uni.createFrom().item { mockResponseDto }
        }

        val result =
            RestAssured.given()
                .header("x-client-id", "CHECKOUT")
                .header("x-api-key", "test-primary")
                .queryParam("amount", "100")
                .`when`()
                .get("$securedPath/payment-methods")
                .then()
                .statusCode(200)
                .extract()
                .`as`(PaymentMethodsResponse::class.java)

        assertEquals(expectedBody, result)
    }

    @Test
    fun shouldHandleCustomException() {
        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull()))
            .thenReturn(Uni.createFrom().failure(PaymentMethodsClientException("test")))

        val expectedProblem = ProblemJson()
        expectedProblem.status = Response.Status.INTERNAL_SERVER_ERROR.statusCode
        expectedProblem.title = "Unexpected Exception"
        expectedProblem.detail = "Error during GMP communication"

        val result =
            RestAssured.given()
                .header("x-client-id", "CHECKOUT")
                .header("x-api-key", "test-primary")
                .queryParam("amount", "100")
                .`when`()
                .get("$securedPath/payment-methods")
                .then()
                .statusCode(500)
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
                .header("x-client-id", "CHECKOUT")
                .header("x-api-key", "test-primary")
                .queryParam("amount", "100")
                .`when`()
                .get("$securedPath/payment-methods")
                .then()
                .statusCode(500)
                .extract()
                .`as`(ProblemJson::class.java)

        assertEquals(expectedProblem, result)
    }
}
