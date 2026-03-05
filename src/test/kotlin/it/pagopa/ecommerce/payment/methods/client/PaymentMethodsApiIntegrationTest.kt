package it.pagopa.ecommerce.payment.methods.client

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.quarkus.test.junit.QuarkusTest
import it.pagopa.generated.ecommerce.client.api.CalculatorApi
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.BundleOptionDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentNoticeItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentOptionMultiDto
import it.pagopa.generated.ecommerce.client.model.PspSearchCriteriaDto
import it.pagopa.generated.ecommerce.client.model.TransferListItemDto
import jakarta.inject.Inject
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@QuarkusTest
class PaymentMethodsApiIntegrationTest {

    @Inject @RestClient lateinit var paymentMethodsApi: PaymentMethodsApi

    @Inject @RestClient lateinit var calculatorApi: CalculatorApi

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

    @Test
    fun `should receive mocked payment method by id response`() {
        val methodId = "pm-001"
        val mockResponse =
            """
            {
              "paymentMethodId": "$methodId",
              "name": { "it": "Carta di credito", "en": "Credit Card" },
              "description": { "it": "Pagamento con carta", "en": "Pay with card" },
              "status": "ENABLED",
              "validityDateFrom": "2025-01-01",
              "group": "CP",
              "paymentMethodTypes": ["CARTE"],
              "paymentMethodAsset": "asset.png",
              "methodManagement": "ONBOARDABLE",
              "paymentMethodsBrandAssets": { "VISA": "visa.png" },
              "metadata": { "priority": "high" }
            }
        """
                .trimIndent()

        wireMock.stubFor(
            get(urlEqualTo("/payment-methods/$methodId"))
                .withHeader("X-Request-Id", equalTo("test-id"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)
                )
        )

        val response: PaymentMethodResponseDto =
            paymentMethodsApi.getPaymentMethod(methodId, "test-id").await().indefinitely()

        assertNotNull(response)
        assertEquals(methodId, response.paymentMethodId)
        assertEquals("Carta di credito", response.name?.get("it"))
    }

    @Test
    fun `should receive mocked calculate fees response`() {
        val mockResponse =
            """
        {
          "belowThreshold": false,
          "bundleOptions": [
            {
              "abi": "abi-1",
              "bundleDescription": "desc",
              "bundleName": "bundle",
              "idBrokerPsp": "broker",
              "idBundle": "bundle-id",
              "idChannel": "channel",
              "idPsp": "psp-1",
              "onUs": false,
              "paymentMethod": "CP",
              "taxPayerFee": 100,
              "touchpoint": "CHECKOUT",
              "pspBusinessName": "PSP Name"
            }
          ]
        }
        """
                .trimIndent()

        wireMock.stubFor(
            post(urlPathEqualTo("/fees"))
                .withHeader("X-Request-Id", equalTo("test-id"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)
                )
        )

        val requestDto =
            PaymentOptionMultiDto().apply {
                bin = "123456"
                idPspList = listOf(PspSearchCriteriaDto().apply { idPsp = "psp-1" })
                paymentMethod = "CP"
                touchpoint = "CHECKOUT"
                paymentNotice =
                    listOf(
                        PaymentNoticeItemDto().apply {
                            paymentAmount = 100L
                            primaryCreditorInstitution = "777777777"
                            transferList =
                                listOf(
                                    TransferListItemDto().apply {
                                        creditorInstitution = "777777777"
                                        transferCategory = "test"
                                        digitalStamp = false
                                    }
                                )
                        }
                    )
            }

        val response: BundleOptionDto =
            calculatorApi
                .getFeesMulti(requestDto, "test-id", Int.MAX_VALUE, "false", null, null)
                .await()
                .indefinitely()

        assertNotNull(response)
        assertEquals(false, response.belowThreshold)
        assertEquals(1, response.bundleOptions?.size)
        assertEquals("psp-1", response.bundleOptions?.get(0)?.idPsp)
        assertEquals(100L, response.bundleOptions?.get(0)?.taxPayerFee)
    }
}
