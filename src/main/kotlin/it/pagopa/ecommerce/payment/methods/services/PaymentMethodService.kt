package it.pagopa.ecommerce.payment.methods.services

import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import java.util.concurrent.CompletionStage

interface PaymentMethodService {
    fun searchPaymentMethods(
        paymentMethodsRequest: PaymentMethodsRequest,
        xRequestId: String,
    ): CompletionStage<PaymentMethodsResponse>

    fun getPaymentMethod(
        paymentMethodsId: String,
        xRequestId: String,
    ): CompletionStage<PaymentMethodResponse>
}
