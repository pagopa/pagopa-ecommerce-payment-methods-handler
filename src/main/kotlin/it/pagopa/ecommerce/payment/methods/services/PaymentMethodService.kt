package it.pagopa.ecommerce.payment.methods.services

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.v1.server.model.CreateSessionResponse
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
        xClientId: String,
    ): CompletionStage<PaymentMethodResponse>

    fun createSessionForPaymentMethod(
        paymentMethodId: String,
        language: String?,
        xClientId: String?,
    ): Uni<CreateSessionResponse>
}
