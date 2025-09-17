package it.pagopa.ecommerce.payment.methods

import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
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
    }
}
