package it.pagopa.ecommerce.payment.methods

import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentNoticeItem

class TestUtils {
    companion object {
        fun buildDefaultMockRequest(): PaymentMethodsRequest {
            return PaymentMethodsRequest()
                .userTouchpoint(PaymentMethodsRequest.UserTouchpointEnum.CHECKOUT)
                .totalAmount(100L)
                .paymentNotice(
                    listOf(
                        PaymentNoticeItem()
                            .paymentAmount(100L)
                            .primaryCreditorInstitution("777777777")
                    )
                )
        }
    }
}
