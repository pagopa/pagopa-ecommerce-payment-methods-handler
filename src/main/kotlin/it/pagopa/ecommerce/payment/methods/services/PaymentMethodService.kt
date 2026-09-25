package it.pagopa.ecommerce.payment.methods.services

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.v1.server.model.CalculateFeeRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.CalculateFeeResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.CreateSessionResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PatchSessionRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.SessionPaymentMethodResponse
import java.util.concurrent.CompletionStage

interface PaymentMethodService {
    fun searchPaymentMethods(
        paymentMethodsRequest: PaymentMethodsRequest,
        xRequestId: String,
    ): CompletionStage<PaymentMethodsResponse>

    fun getPaymentMethod(
        paymentMethodsId: String,
        xRequestId: String,
        xClientId: String?,
    ): CompletionStage<PaymentMethodResponse>

    fun calculateFees(
        paymentMethodsId: String,
        calculateFeeRequest: CalculateFeeRequest,
        xRequestId: String,
        xClientId: String,
        xLanguage: String,
        maxOccurrences: Int,
    ): CompletionStage<CalculateFeeResponse>

    fun createSessionForPaymentMethod(
        paymentMethodId: String,
        language: String?,
        xClientId: String,
    ): Uni<CreateSessionResponse>

    fun getCardDataInformation(
        paymentMethodId: String,
        orderId: String,
        xClientId: String,
    ): Uni<SessionPaymentMethodResponse>

    fun updateSession(
        paymentMethodId: String,
        orderId: String,
        patchSessionRequest: PatchSessionRequest,
        xClientId: String,
    ): Uni<Void>
}
