package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.NpgResponseException
import java.net.URI
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NpgClientWrapperTest {

    private val npgRestClient = mock<NpgRestClient>()
    private val apiKey = "test-npg-api-key"
    private val npgClientWrapper = NpgClientWrapper(npgRestClient, apiKey)

    private val correlationId = UUID.randomUUID()
    private val merchantUrl = URI.create("https://checkout.pagopa.it")
    private val resultUrl = URI.create("https://checkout.pagopa.it/esito")
    private val notificationUrl = URI.create("https://api.pagopa.it/notifications/order1/token1")
    private val cancelUrl = URI.create("https://checkout.pagopa.it/annulla")
    private val orderId = "E1234567890123abcd"

    @Test
    fun `should build form successfully`() {
        val npgResponse =
            NpgBuildResponse(
                sessionId = "session-123",
                securityToken = "sec-token-456",
                fields =
                    listOf(
                        NpgBuildFieldResponse(
                            id = "cardholderName",
                            type = "text",
                            propertyClass = "cardData",
                            src = "https://fe.npg.it/field.html?id=CARDHOLDER_NAME",
                        ),
                        NpgBuildFieldResponse(
                            id = "cardNumber",
                            type = "text",
                            propertyClass = "cardData",
                            src = "https://fe.npg.it/field.html?id=CARD_NUMBER",
                        ),
                    ),
                state = "GDI_VERIFICATION",
            )

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .buildForm(any(), any(), any())

        val result =
            npgClientWrapper
                .buildForm(
                    correlationId = correlationId,
                    merchantUrl = merchantUrl,
                    resultUrl = resultUrl,
                    notificationUrl = notificationUrl,
                    cancelUrl = cancelUrl,
                    orderId = orderId,
                    paymentMethod = NpgPaymentMethod.CARDS,
                    language = "it",
                )
                .await()
                .indefinitely()

        assertEquals("session-123", result.sessionId)
        assertEquals("sec-token-456", result.securityToken)
        assertEquals(2, result.fields.size)
        assertEquals("cardholderName", result.fields[0].id)
        assertEquals("cardNumber", result.fields[1].id)
    }

    @Test
    fun `should pass correct authorization header with Bearer prefix`() {
        val npgResponse =
            NpgBuildResponse(
                sessionId = "session-123",
                securityToken = "sec-token-456",
                fields = emptyList(),
                state = null,
            )

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .buildForm(any(), any(), any())

        npgClientWrapper
            .buildForm(
                correlationId = correlationId,
                merchantUrl = merchantUrl,
                resultUrl = resultUrl,
                notificationUrl = notificationUrl,
                cancelUrl = cancelUrl,
                orderId = orderId,
                paymentMethod = NpgPaymentMethod.CARDS,
                language = null,
            )
            .await()
            .indefinitely()

        verify(npgRestClient).buildForm(eq(correlationId.toString()), eq("Bearer $apiKey"), any())
    }

    @Test
    fun `should throw NpgResponseException when sessionId is null`() {
        val npgResponse =
            NpgBuildResponse(
                sessionId = null,
                securityToken = "sec-token",
                fields = emptyList(),
                state = null,
            )

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .buildForm(any(), any(), any())

        val thrown =
            assertThrows<NpgResponseException> {
                npgClientWrapper
                    .buildForm(
                        correlationId = correlationId,
                        merchantUrl = merchantUrl,
                        resultUrl = resultUrl,
                        notificationUrl = notificationUrl,
                        cancelUrl = cancelUrl,
                        orderId = orderId,
                        paymentMethod = NpgPaymentMethod.CARDS,
                        language = null,
                    )
                    .await()
                    .indefinitely()
            }

        assertEquals("Missing sessionId in NPG response", thrown.message)
    }

    @Test
    fun `should throw NpgResponseException when securityToken is null`() {
        val npgResponse =
            NpgBuildResponse(
                sessionId = "session-123",
                securityToken = null,
                fields = emptyList(),
                state = null,
            )

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .buildForm(any(), any(), any())

        val thrown =
            assertThrows<NpgResponseException> {
                npgClientWrapper
                    .buildForm(
                        correlationId = correlationId,
                        merchantUrl = merchantUrl,
                        resultUrl = resultUrl,
                        notificationUrl = notificationUrl,
                        cancelUrl = cancelUrl,
                        orderId = orderId,
                        paymentMethod = NpgPaymentMethod.CARDS,
                        language = null,
                    )
                    .await()
                    .indefinitely()
            }

        assertEquals("Missing securityToken in NPG response", thrown.message)
    }

    @Test
    fun `should return empty fields list when NPG response fields is null`() {
        val npgResponse =
            NpgBuildResponse(
                sessionId = "session-123",
                securityToken = "sec-token-456",
                fields = null,
                state = null,
            )

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .buildForm(any(), any(), any())

        val result =
            npgClientWrapper
                .buildForm(
                    correlationId = correlationId,
                    merchantUrl = merchantUrl,
                    resultUrl = resultUrl,
                    notificationUrl = notificationUrl,
                    cancelUrl = cancelUrl,
                    orderId = orderId,
                    paymentMethod = NpgPaymentMethod.CARDS,
                    language = null,
                )
                .await()
                .indefinitely()

        assertTrue(result.fields.isEmpty())
    }

    @Test
    fun `should propagate error when NPG rest client fails`() {
        val restError = RuntimeException("NPG connection timeout")

        doReturn(Uni.createFrom().failure<NpgBuildResponse>(restError))
            .whenever(npgRestClient)
            .buildForm(any(), any(), any())

        val thrown =
            assertThrows<RuntimeException> {
                npgClientWrapper
                    .buildForm(
                        correlationId = correlationId,
                        merchantUrl = merchantUrl,
                        resultUrl = resultUrl,
                        notificationUrl = notificationUrl,
                        cancelUrl = cancelUrl,
                        orderId = orderId,
                        paymentMethod = NpgPaymentMethod.CARDS,
                        language = null,
                    )
                    .await()
                    .indefinitely()
            }

        assertEquals("NPG connection timeout", thrown.message)
    }
}
