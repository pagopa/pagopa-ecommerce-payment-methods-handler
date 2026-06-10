package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.NpgResponseException
import it.pagopa.ecommerce.payment.methods.v1.server.model.NpgBuildFormParams
import it.pagopa.ecommerce.payment.methods.v1.server.model.NpgSessionUrls
import it.pagopa.generated.npg.client.api.PaymentServicesApi
import it.pagopa.generated.npg.client.model.FieldDto
import it.pagopa.generated.npg.client.model.FieldsDto
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

    private val npgRestClient = mock<PaymentServicesApi>()
    private val apiKey = "test-npg-api-key"
    private val npgClientWrapper = NpgClientWrapper(npgRestClient, apiKey)

    private val correlationId = UUID.randomUUID()

    private val defaultUrls =
        NpgSessionUrls().apply {
            merchantUrl = URI.create("https://checkout.pagopa.it")
            resultUrl = URI.create("https://checkout.pagopa.it/esito")
            notificationUrl = URI.create("https://api.pagopa.it/notifications/order1/token1")
            cancelUrl = URI.create("https://checkout.pagopa.it/annulla")
        }

    private val orderId = "E1234567890123abcd"

    private fun buildParams(language: String? = "it") =
        NpgBuildFormParams().apply {
            this.correlationId = this@NpgClientWrapperTest.correlationId
            urls = defaultUrls
            this.orderId = this@NpgClientWrapperTest.orderId
            paymentMethod = NpgPaymentMethod.CARDS.serviceName
            this.language = language
        }

    private fun buildFieldsDto(
        sessionId: String? = "session-123",
        securityToken: String? = "sec-token-456",
        fields: List<FieldDto>? = emptyList(),
    ): FieldsDto =
        FieldsDto().apply {
            this.sessionId = sessionId
            this.securityToken = securityToken
            this.fields = fields
        }

    @Test
    fun `should build form successfully`() {
        val field1 =
            FieldDto().apply {
                id = "cardholderName"
                type = "text"
                propertyClass = "cardData"
                src = "https://fe.npg.it/field.html?id=CARDHOLDER_NAME"
            }
        val field2 =
            FieldDto().apply {
                id = "cardNumber"
                type = "text"
                propertyClass = "cardData"
                src = "https://fe.npg.it/field.html?id=CARD_NUMBER"
            }
        val npgResponse = buildFieldsDto(fields = listOf(field1, field2))

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .pspApiV1OrdersBuildPost(any(), any(), any())

        val result = npgClientWrapper.buildForm(buildParams()).await().indefinitely()

        assertEquals("session-123", result.sessionId)
        assertEquals("sec-token-456", result.securityToken)
        assertEquals(2, result.fields.size)
        assertEquals("cardholderName", result.fields[0].id)
        assertEquals("cardNumber", result.fields[1].id)
    }

    @Test
    fun `should pass correct authorization header with Bearer prefix`() {
        val npgResponse = buildFieldsDto()

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .pspApiV1OrdersBuildPost(any(), any(), any())

        npgClientWrapper.buildForm(buildParams(language = null)).await().indefinitely()

        verify(npgRestClient)
            .pspApiV1OrdersBuildPost(eq(correlationId), eq(apiKey), any())
    }

    @Test
    fun `should throw NpgResponseException when sessionId is null`() {
        val npgResponse = buildFieldsDto(sessionId = null)

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .pspApiV1OrdersBuildPost(any(), any(), any())

        val thrown =
            assertThrows<NpgResponseException> {
                npgClientWrapper.buildForm(buildParams(language = null)).await().indefinitely()
            }

        assertEquals("Missing sessionId in NPG response", thrown.message)
    }

    @Test
    fun `should throw NpgResponseException when securityToken is null`() {
        val npgResponse = buildFieldsDto(sessionId = "session-123", securityToken = null)

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .pspApiV1OrdersBuildPost(any(), any(), any())

        val thrown =
            assertThrows<NpgResponseException> {
                npgClientWrapper.buildForm(buildParams(language = null)).await().indefinitely()
            }

        assertEquals("Missing securityToken in NPG response", thrown.message)
    }

    @Test
    fun `should return null fields list when NPG response fields is null`() {
        val npgResponse = buildFieldsDto(fields = null)

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .pspApiV1OrdersBuildPost(any(), any(), any())

        val result = npgClientWrapper.buildForm(buildParams(language = null)).await().indefinitely()

        assertTrue(result.fields == null)
    }

    @Test
    fun `should use default language when language is not in langMap`() {
        val npgResponse = buildFieldsDto()

        doReturn(Uni.createFrom().item(npgResponse))
            .whenever(npgRestClient)
            .pspApiV1OrdersBuildPost(any(), any(), any())

        val result = npgClientWrapper.buildForm(buildParams(language = "xx")).await().indefinitely()

        assertEquals("session-123", result.sessionId)
    }

    @Test
    fun `should propagate error when NPG rest client fails`() {
        val restError = RuntimeException("NPG connection timeout")

        doReturn(Uni.createFrom().failure<FieldsDto>(restError))
            .whenever(npgRestClient)
            .pspApiV1OrdersBuildPost(any(), any(), any())

        val thrown =
            assertThrows<NpgResponseException> {
                npgClientWrapper.buildForm(buildParams(language = null)).await().indefinitely()
            }

        assertTrue(thrown.message!!.contains("NPG connection timeout"))
    }
}
