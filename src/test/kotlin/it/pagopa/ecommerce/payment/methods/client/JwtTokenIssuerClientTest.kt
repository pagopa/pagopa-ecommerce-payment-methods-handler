package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.JwtIssuerResponseException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class JwtTokenIssuerClientTest {

    private val apiKey = "test-api-key"
    private val jwtIssuerRestClient = mock<JwtTokenIssuerRestClient>()
    private val jwtTokenIssuerClient = JwtTokenIssuerClient(jwtIssuerRestClient, apiKey)

    private val defaultRequest =
        CreateTokenRequest(
            privateClaims =
                mapOf(
                    JwtTokenIssuerClient.ORDER_ID_CLAIM to "order-123",
                    JwtTokenIssuerClient.PAYMENT_METHOD_ID_CLAIM to "pm-456",
                ),
            audience = JwtTokenIssuerClient.NPG_AUDIENCE,
            duration = 900,
        )

    @Test
    fun `should create JWT token successfully`() {
        val expectedResponse = CreateTokenResponse(token = "jwt-token-value")

        doReturn(Uni.createFrom().item(expectedResponse))
            .whenever(jwtIssuerRestClient)
            .createJwtToken(eq(apiKey), any())

        val result = jwtTokenIssuerClient.createJWTToken(defaultRequest).await().indefinitely()

        assertEquals("jwt-token-value", result.token)
        verify(jwtIssuerRestClient).createJwtToken(eq(apiKey), eq(defaultRequest))
    }

    @Test
    fun `should wrap error in JwtIssuerResponseException on failure`() {
        val simulatedError = RuntimeException("Connection refused")

        doReturn(Uni.createFrom().failure<CreateTokenResponse>(simulatedError))
            .whenever(jwtIssuerRestClient)
            .createJwtToken(eq(apiKey), any())

        val thrown =
            assertThrows<JwtIssuerResponseException> {
                jwtTokenIssuerClient.createJWTToken(defaultRequest).await().indefinitely()
            }

        assertEquals("Error while invoking JWT token creation", thrown.message)
        assertNotNull(thrown.cause)
        assertEquals("Connection refused", thrown.cause!!.message)
    }

    @Test
    fun `should pass correct api key header to rest client`() {
        val expectedResponse = CreateTokenResponse(token = "token")

        doReturn(Uni.createFrom().item(expectedResponse))
            .whenever(jwtIssuerRestClient)
            .createJwtToken(eq(apiKey), any())

        jwtTokenIssuerClient.createJWTToken(defaultRequest).await().indefinitely()

        verify(jwtIssuerRestClient).createJwtToken(eq(apiKey), eq(defaultRequest))
    }

    @Test
    fun `should pass correct claims in request`() {
        val expectedResponse = CreateTokenResponse(token = "token")

        doReturn(Uni.createFrom().item(expectedResponse))
            .whenever(jwtIssuerRestClient)
            .createJwtToken(eq(apiKey), any())

        jwtTokenIssuerClient.createJWTToken(defaultRequest).await().indefinitely()

        verify(jwtIssuerRestClient)
            .createJwtToken(
                eq(apiKey),
                eq(
                    CreateTokenRequest(
                        privateClaims =
                            mapOf("orderId" to "order-123", "paymentMethodId" to "pm-456"),
                        audience = "npg",
                        duration = 900,
                    )
                ),
            )
    }
}
