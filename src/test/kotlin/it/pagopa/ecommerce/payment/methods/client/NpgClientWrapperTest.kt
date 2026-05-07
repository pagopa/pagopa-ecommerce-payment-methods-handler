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

    private val defaultUrls =
        NpgSessionUrls(
            merchantUrl = URI.create("https://checkout.pagopa.it"),
            resultUrl = URI.create("https://checkout.pagopa.it/esito"),
            notificationUrl = URI.create("https://api.pagopa.it/notifications/order1/token1"),
            cancelUrl = URI.create("https://checkout.pagopa.it/annulla"),
        )

    private val orderId = "E1234567890123abcd"

    private fun buildParams(language: String? = "it") =
        NpgBuildFormParams(
            correlationId = correlationId,
            urls = defaultUrls,
            orderId = orderId,
            paymentMethod = NpgPaymentMethod.CARDS,
            language = language,
        )

    @Test
    fun `should build form successfully`() {
        val npgResponse =
            NpgBuildResponse(
                sessionId = "session-123",
                securityToken = "sec-token-456",
                fields =
                    listOf(
                        NpgBuildFieldResponse(
                            "cardholderName",
                            "text",
                            "cardData",
                            "https://fe.npg.it/field.html?id=CARDHOLDER_NAME",
                        ),
                        NpgBuildFieldResponse(
                            "cardNumber",
                            "text",
                            "cardData",
                            "https://fe.npg.it/field.html?id=CARD_NUMBER",
                        ),
                    ),
                state = "GDI_VERIFICATION",
            )

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .buildForm(any(), any(), any())

        val result = npgClientWrapper.buildForm(buildParams()).await().indefinitely()

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

        npgClientWrapper.buildForm(buildParams(language = null)).await().indefinitely()

        verify(npgRestClient).buildForm(eq(correlationId.toString()), eq(apiKey), any())
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
                npgClientWrapper.buildForm(buildParams(language = null)).await().indefinitely()
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
                npgClientWrapper.buildForm(buildParams(language = null)).await().indefinitely()
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

        val result = npgClientWrapper.buildForm(buildParams(language = null)).await().indefinitely()

        assertTrue(result.fields.isEmpty())
    }

    @Test
    fun `should use default language when language is not in langMap`() {
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

        val result = npgClientWrapper.buildForm(buildParams(language = "xx")).await().indefinitely()

        assertEquals("session-123", result.sessionId)
    }

    @Test
    fun `should propagate error when NPG rest client fails`() {
        val restError = RuntimeException("NPG connection timeout")

        doReturn(Uni.createFrom().failure<NpgBuildResponse>(restError))
            .whenever(npgRestClient)
            .buildForm(any(), any(), any())

        val thrown =
            assertThrows<NpgResponseException> {
                npgClientWrapper.buildForm(buildParams(language = null)).await().indefinitely()
            }

        assertTrue(thrown.message!!.contains("NPG connection timeout"))
    }

    // --- getCardData tests ---

    @Test
    fun `should get card data successfully`() {
        val sessionId = "session-123"
        val npgResponse =
            NpgCardDataResponse(
                bin = "123456",
                lastFourDigits = "7890",
                expiringDate = "1225",
                circuit = "VISA",
            )

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .getCardData(any(), any(), any())

        val result = npgClientWrapper.getCardData(correlationId, sessionId).await().indefinitely()

        assertEquals("123456", result.bin)
        assertEquals("7890", result.lastFourDigits)
        assertEquals("1225", result.expiringDate)
        assertEquals("VISA", result.circuit)

        verify(npgRestClient).getCardData(eq(correlationId.toString()), eq(apiKey), eq(sessionId))
    }

    @Test
    fun `should propagate error when NPG getCardData fails`() {
        val restError = RuntimeException("NPG getCardData timeout")

        doReturn(Uni.createFrom().failure<NpgCardDataResponse>(restError))
            .whenever(npgRestClient)
            .getCardData(any(), any(), any())

        val thrown =
            assertThrows<NpgResponseException> {
                npgClientWrapper.getCardData(correlationId, "session-123").await().indefinitely()
            }

        assertTrue(thrown.message!!.contains("NPG getCardData timeout"))
    }

    @Test
    fun `should propagate NpgResponseException as-is from getCardData`() {
        val npgError = NpgResponseException("NPG card data error")

        doReturn(Uni.createFrom().failure<NpgCardDataResponse>(npgError))
            .whenever(npgRestClient)
            .getCardData(any(), any(), any())

        val thrown =
            assertThrows<NpgResponseException> {
                npgClientWrapper.getCardData(correlationId, "session-123").await().indefinitely()
            }

        assertEquals("NPG card data error", thrown.message)
    }
}
