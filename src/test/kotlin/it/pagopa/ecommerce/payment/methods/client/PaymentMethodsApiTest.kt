package it.pagopa.ecommerce.payment.methods.client

import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentNoticeItemDto
import it.pagopa.generated.ecommerce.client.model.TransferListItemDto
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@QuarkusTest
class PaymentMethodsApiTest {

    @Test
    fun `should return payment methods`() {

        val transfer1 =
            TransferListItemDto().apply {
                creditorInstitution = "Comune di Rende"
                transferCategory = "TARI"
                digitalStamp = false
            }

        val transfer2 =
            TransferListItemDto().apply {
                creditorInstitution = "Regione Calabria"
                transferCategory = "IMU"
                digitalStamp = true
            }

        val noticeItem =
            PaymentNoticeItemDto().apply {
                paymentAmount = 2500L
                primaryCreditorInstitution = "Comune di Rende"
                transferList = listOf(transfer1, transfer2)
            }

        val requestDto =
            PaymentMethodRequestDto().apply {
                userTouchpoint = PaymentMethodRequestDto.UserTouchpointEnum.CHECKOUT
                userDevice = PaymentMethodRequestDto.UserDeviceEnum.WEB
                bin = "457865"
                totalAmount = 2500
                allCCp = true
                targetKey = "TAX_2025_RENDE"
                paymentNotice = listOf(noticeItem)
            }

        val item1 =
            PaymentMethodsItemDto().apply {
                paymentMethodId = "card_visa"
                name = mapOf("it" to "Carta Visa", "en" to "Visa Card")
                description =
                    mapOf("it" to "Pagamento con carta di credito", "en" to "Credit card payment")
                status = PaymentMethodsItemDto.StatusEnum.ENABLED
                validityDateFrom = LocalDate.of(2025, 1, 1)
                group = PaymentMethodsItemDto.GroupEnum.CP
                paymentMethodTypes = listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.CARTE)
                paymentMethodAsset = "visa.png"
                methodManagement = PaymentMethodsItemDto.MethodManagementEnum.ONBOARDABLE
                disabledReason = null
                paymentMethodsBrandAssets = mapOf("logo" to "visa.svg")
                metadata = mapOf("priority" to "high")
            }

        val item2 =
            PaymentMethodsItemDto().apply {
                paymentMethodId = "paypal"
                name = mapOf("it" to "PayPal", "en" to "PayPal")
                description = mapOf("it" to "Pagamento con PayPal", "en" to "Pay with PayPal")
                status = PaymentMethodsItemDto.StatusEnum.ENABLED
                validityDateFrom = LocalDate.of(2025, 1, 1)
                group = PaymentMethodsItemDto.GroupEnum.PPAL
                paymentMethodTypes = listOf(PaymentMethodsItemDto.PaymentMethodTypesEnum.APP)
                paymentMethodAsset = "paypal.png"
                methodManagement = PaymentMethodsItemDto.MethodManagementEnum.REDIRECT
                disabledReason = null
                paymentMethodsBrandAssets = mapOf("logo" to "paypal.svg")
                metadata = mapOf("priority" to "medium")
            }

        val expectedResponse =
            PaymentMethodsResponseDto().apply { paymentMethods = listOf(item1, item2) }

        val mockApi = Mockito.mock(PaymentMethodsApi::class.java)
        whenever(mockApi.searchPaymentMethods(requestDto, "test-request-id"))
            .thenReturn(Uni.createFrom().item(expectedResponse))

        val response =
            mockApi.searchPaymentMethods(requestDto, "test-request-id").await().indefinitely()

        assertEquals(2, response.paymentMethods?.size)
        assertEquals("card_visa", response.paymentMethods?.get(0)?.paymentMethodId)
        assertEquals("Carta Visa", response.paymentMethods?.get(0)?.name?.get("it"))
        assertEquals("PayPal", response.paymentMethods?.get(1)?.name?.get("it"))
        assertEquals(
            PaymentMethodsItemDto.MethodManagementEnum.REDIRECT,
            response.paymentMethods?.get(1)?.methodManagement,
        )
    }
}
