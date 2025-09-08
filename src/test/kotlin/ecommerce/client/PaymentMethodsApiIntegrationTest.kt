package ecommerce.client

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.quarkus.test.junit.QuarkusTest
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import jakarta.inject.Inject
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@QuarkusTest
class PaymentMethodsApiIntegrationTest {

    @Inject @RestClient lateinit var paymentMethodsApi: PaymentMethodsApi

    companion object {
        @JvmStatic
        @RegisterExtension
        val wireMock =
            WireMockExtension.newInstance()
                .options(WireMockConfiguration.wireMockConfig().port(8089))
                .build()
    }

    @Test
    fun `should receive mocked payment methods response`() {
        val mockResponse =
            """
            {
              "paymentMethods": [
                {
                  "paymentMethodId": "pm-001",
                  "name": { "it": "Carta di credito", "en": "Credit Card" },
                  "description": { "it": "Pagamento con carta", "en": "Pay with card" },
                  "status": "ENABLED",
                  "validityDateFrom": "2025-01-01",
                  "group": "CP",
                  "paymentMethodTypes": ["CARTE"],
                  "paymentMethodAsset": "asset.png",
                  "methodManagement": "ONBOARDABLE",
                  "disabledReason": null,
                  "paymentMethodsBrandAssets": { "VISA": "visa.png" },
                  "metadata": { "priority": "high" }
                }
              ]
            }
        """
                .trimIndent()

        wireMock.stubFor(
            post(urlEqualTo("/payment-methods/search"))
                .withHeader("X-Request-Id", equalTo("test-id"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)
                )
        )

        val requestDto =
            PaymentMethodRequestDto().apply {
                userTouchpoint = PaymentMethodRequestDto.UserTouchpointEnum.CHECKOUT
                userDevice = PaymentMethodRequestDto.UserDeviceEnum.WEB
                bin = "457865"
                totalAmount = 2500
                allCCp = true
                targetKey = "MOCK_TEST"
            }

        val response: PaymentMethodsResponseDto =
            paymentMethodsApi.searchPaymentMethods(requestDto, "test-id").await().indefinitely()

        assertNotNull(response)
        assertEquals(1, response.paymentMethods?.size)
        assertEquals("pm-001", response.paymentMethods?.get(0)?.paymentMethodId)
        assertEquals("Carta di credito", response.paymentMethods?.get(0)?.name?.get("it"))
    }
}
