package it.pagopa.ecommerce.payment.methods.services

import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@QuarkusTest
class PaymentMethodsClientTest {

    private val mockClient = Mockito.mock(PaymentMethodsClient::class.java)
    private val service = PaymentMethodServiceImpl(mockClient)

    private val mockApi = Mockito.mock(PaymentMethodsApi::class.java)
    private val client = PaymentMethodsClient(mockApi)

    @Test
    fun `should return response from PaymentMethodsApi`() {
        val requestDto =
            PaymentMethodRequestDto().apply {
                userTouchpoint = PaymentMethodRequestDto.UserTouchpointEnum.CHECKOUT
                userDevice = PaymentMethodRequestDto.UserDeviceEnum.WEB
                bin = "457865"
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
            }

        val expectedResponse =
            PaymentMethodsResponseDto().apply {
                paymentMethods =
                    listOf(
                        PaymentMethodsItemDto().apply {
                            paymentMethodId = "card_visa"
                            name = mapOf("it" to "Carta Visa")
                            status = PaymentMethodsItemDto.StatusEnum.ENABLED
                            validityDateFrom = LocalDate.of(2025, 1, 1)
                            group = PaymentMethodsItemDto.GroupEnum.CP
                            paymentMethodTypes =
                                listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.CARTE)
                            methodManagement =
                                PaymentMethodsItemDto.MethodManagementEnum.ONBOARDABLE
                        }
                    )
            }

        whenever(mockApi.searchPaymentMethods(requestDto, "test-id"))
            .thenReturn(Uni.createFrom().item(expectedResponse))

        val response = client.searchPaymentMethods(requestDto, "test-id").await().indefinitely()

        assertEquals(1, response.paymentMethods?.size)
        assertEquals("card_visa", response.paymentMethods?.get(0)?.paymentMethodId)
        assertEquals("Carta Visa", response.paymentMethods?.get(0)?.name?.get("it"))
    }

    @Test
    fun `should handle failure from PaymentMethodsApi`() {
        val requestDto = PaymentMethodRequestDto()
        val simulatedError = RuntimeException("API failure")

        whenever(mockApi.searchPaymentMethods(requestDto, "test-id"))
            .thenReturn(Uni.createFrom().failure(simulatedError))

        val thrown =
            assertThrows<PaymentMethodsClientException> {
                client.searchPaymentMethods(requestDto, "test-id").await().indefinitely()
            }

        assertEquals("Error during the call to PaymentMethodsApi", thrown.message)
        assertEquals(simulatedError, thrown.cause)
    }

    @Test
    fun `should delegate searchPaymentMethods to client`() {
        val amount = BigDecimal(100)
        val expectedResponse = PaymentMethodsResponse()
        val expectedResponseDto = PaymentMethodsResponseDto()

        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull()))
            .thenReturn(Uni.createFrom().item(expectedResponseDto))

        val result =
            service.searchPaymentMethods(amount, "CHECKOUT", "test-id").toCompletableFuture().get()

        assertEquals(expectedResponse, result)
    }
}
