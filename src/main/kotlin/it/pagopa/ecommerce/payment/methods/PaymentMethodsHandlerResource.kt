package it.pagopa.ecommerce.payment.methods

import it.pagopa.ecommerce.payment.methods.v1.server.api.PaymentMethodsApi
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

class PaymentMethodsHandlerResource : PaymentMethodsApi {
    override fun getAllPaymentMethods(
        xClientId: @NotNull String?,
        amount: BigDecimal?,
    ): PaymentMethodsResponse? {
        return PaymentMethodsResponse()
    }
}
