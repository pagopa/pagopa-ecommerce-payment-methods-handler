package it.pagopa.ecommerce.payment.methods.services

import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.TestUtils
import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodNotFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import jakarta.ws.rs.core.Response
import java.time.LocalDate
import org.jboss.resteasy.reactive.ClientWebApplicationException
import org.jboss.resteasy.reactive.client.impl.ClientResponseImpl
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
    fun `should return response from PaymentMethodsApi get all methods`() {
        val requestDto =
            PaymentMethodRequestDto().apply {
                userTouchpoint = PaymentMethodRequestDto.UserTouchpointEnum.CHECKOUT
                userDevice = PaymentMethodRequestDto.UserDeviceEnum.WEB
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
                            validityDateFrom = LocalDate.now()
                            disabledReason = null
                            metadata = mapOf("test" to "test")
                            paymentMethodTypes =
                                listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.CARTE)
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
    fun `should return payment not found exception when payment method does not exist`() {
        val methodId = "test-id"
        val mockResponse = ClientResponseImpl()
        mockResponse.setStatus(Response.Status.NOT_FOUND.statusCode)

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(Uni.createFrom().failure(ClientWebApplicationException(mockResponse)))

        assertThrows<PaymentMethodNotFoundException> {
            client.getPaymentMethod(methodId, "test-id", "CHECKOUT").await().indefinitely()
        }
    }

    @Test
    fun `should return response from PaymentMethodsApi get method by id`() {
        val methodId = "test-id"
        val expectedResponse =
            PaymentMethodResponseDto().apply {
                paymentMethodId = "test-id"
                name = mapOf("it" to "Carta Visa")
                status = PaymentMethodResponseDto.StatusEnum.ENABLED
                validityDateFrom = LocalDate.of(2025, 1, 1)
                group = PaymentMethodResponseDto.GroupEnum.CP
                paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
                userTouchpoint = listOf(PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT)
                methodManagement = PaymentMethodResponseDto.MethodManagementEnum.ONBOARDABLE
                validityDateFrom = LocalDate.now()
                metadata = mapOf("test" to "test")
                paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
            }

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(Uni.createFrom().item(expectedResponse))

        val response =
            client.getPaymentMethod(methodId, "test-id", "CHECKOUT").await().indefinitely()

        assertEquals("Carta Visa", response.name?.get("it"))
        assertEquals(methodId, response.paymentMethodId)
    }

    @Test
    fun `should handle failure from PaymentMethodsApi get all methods`() {
        val requestDto = PaymentMethodRequestDto()
        val simulatedError = RuntimeException("API failure")

        whenever(mockApi.searchPaymentMethods(requestDto, "test-id"))
            .thenReturn(Uni.createFrom().failure(simulatedError))

        val thrown =
            assertThrows<PaymentMethodsClientException> {
                client.searchPaymentMethods(requestDto, "test-id").await().indefinitely()
            }

        assertEquals(
            "Error during the call to PaymentMethodsApi.searchPaymentMethods",
            thrown.message,
        )
        assertEquals(simulatedError, thrown.cause)
    }

    @Test
    fun `should handle failure from PaymentMethodsApi get method by id`() {
        val methodId = "test-id"
        val simulatedError = RuntimeException("API failure")

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(Uni.createFrom().failure(simulatedError))

        val thrown =
            assertThrows<PaymentMethodsClientException> {
                client.getPaymentMethod(methodId, "test-id", "CHECKOUT").await().indefinitely()
            }

        assertEquals("Error during the call to PaymentMethodsApi.getPaymentMethod", thrown.message)
        assertEquals(simulatedError, thrown.cause)
    }

    @Test
    fun `should delegate searchPaymentMethods to client`() {
        val expectedResponse = PaymentMethodsResponse()
        val expectedResponseDto = PaymentMethodsResponseDto()

        whenever(mockClient.searchPaymentMethods(anyOrNull(), anyOrNull()))
            .thenReturn(Uni.createFrom().item(expectedResponseDto))

        val result =
            service
                .searchPaymentMethods(TestUtils.buildDefaultMockRequest(), "test-id")
                .toCompletableFuture()
                .get()

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `should return PaymentNotFoundException if retrieved method is not for the provided user touchpoint`() {
        val methodId = "test-id"
        val expectedResponse =
            PaymentMethodResponseDto().apply {
                paymentMethodId = "test-id"
                name = mapOf("it" to "Carta Visa")
                status = PaymentMethodResponseDto.StatusEnum.ENABLED
                validityDateFrom = LocalDate.of(2025, 1, 1)
                group = PaymentMethodResponseDto.GroupEnum.CP
                paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
                userTouchpoint = listOf(PaymentMethodResponseDto.UserTouchpointEnum.CHECKOUT)
                methodManagement = PaymentMethodResponseDto.MethodManagementEnum.ONBOARDABLE
                validityDateFrom = LocalDate.now()
                metadata = mapOf("test" to "test")
                paymentMethodTypes = listOf(PaymentMethodResponseDto.PaymentMethodTypesEnum.CARTE)
            }

        whenever(mockApi.getPaymentMethod(methodId, "test-id"))
            .thenReturn(Uni.createFrom().item { expectedResponse })

        assertThrows<PaymentMethodNotFoundException> {
            client.getPaymentMethod(methodId, "test-id", "IO").await().indefinitely()
        }
    }
}
