package it.pagopa.ecommerce.payment.methods.resource.v1

import it.pagopa.ecommerce.payment.methods.v1.server.api.PaymentMethodsApi
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class PaymentMethodsHandlerResource : PaymentMethodsApi {
    override fun getAllPaymentMethods(
        xClientId: @NotNull String?,
        amount: BigDecimal?,
    ): CompletionStage<PaymentMethodsResponse?>? {
        return CompletableFuture.completedFuture<PaymentMethodsResponse>(PaymentMethodsResponse())
    }
}
