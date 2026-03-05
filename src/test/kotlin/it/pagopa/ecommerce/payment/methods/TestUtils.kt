package it.pagopa.ecommerce.payment.methods

import it.pagopa.ecommerce.payment.methods.v1.server.model.CalculateFeeRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.CalculateFeeResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentNotice
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentNoticeItem
import it.pagopa.ecommerce.payment.methods.v1.server.model.TransferListItem

class TestUtils {
    companion object {
        fun buildDefaultMockRequest(): PaymentMethodsRequest {
            return PaymentMethodsRequest()
                .userTouchpoint(PaymentMethodsRequest.UserTouchpointEnum.CHECKOUT)
                .totalAmount(100L)
                .userDevice(PaymentMethodsRequest.UserDeviceEnum.WEB)
                .paymentNotice(
                    listOf(
                        PaymentNoticeItem()
                            .paymentAmount(100L)
                            .primaryCreditorInstitution("777777777")
                            .transferList(
                                listOf(
                                    TransferListItem()
                                        .creditorInstitution("777777777")
                                        .transferCategory("test")
                                        .digitalStamp(false)
                                )
                            )
                    )
                )
        }

        fun buildCalculateFeeRequest(): CalculateFeeRequest =
            CalculateFeeRequest(
                    "CHECKOUT",
                    listOf(
                        PaymentNotice(
                            100L,
                            "777777777",
                            listOf(
                                TransferListItem()
                                    .creditorInstitution("777777777")
                                    .transferCategory("test")
                                    .digitalStamp(false)
                            ),
                        )
                    ),
                    false,
                )
                .bin("123456")
                .idPspList(listOf("psp-1"))

        fun buildCalculateFeeResponse(): CalculateFeeResponse =
            CalculateFeeResponse(
                    "Carte",
                    "Pagamento con carta",
                    CalculateFeeResponse.PaymentMethodStatusEnum.ENABLED,
                    listOf(),
                    "asset-url",
                )
                .apply {
                    belowThreshold = false
                    brandAssets = mapOf("VISA" to "visa-asset")
                }

        fun buildCalculateFeeRequestMultiNotice() =
            CalculateFeeRequest().apply {
                isAllCCP = false
                touchpoint = "CHECKOUT"
                paymentNotices =
                    listOf(
                        PaymentNotice().apply {
                            paymentAmount = 100
                            primaryCreditorInstitution = "77777777777"
                            transferList = emptyList()
                        },
                        PaymentNotice().apply {
                            paymentAmount = 200
                            primaryCreditorInstitution = "77777777777"
                            transferList = emptyList()
                        },
                    )
            }
    }
}
