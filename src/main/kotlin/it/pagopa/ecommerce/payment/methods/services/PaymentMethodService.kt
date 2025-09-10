package it.pagopa.ecommerce.payment.methods.services

import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import java.math.BigDecimal
import java.util.concurrent.CompletionStage

interface PaymentMethodService {
    fun searchPaymentMethods(
        amount: BigDecimal,
        xClientId: String,
        xRequestId: String,
    ): CompletionStage<PaymentMethodsResponse>
}
