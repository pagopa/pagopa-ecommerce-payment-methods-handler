package it.pagopa.ecommerce.payment.methods.resource.v1

import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.services.PaymentMethodService
import it.pagopa.ecommerce.payment.methods.v1.server.api.PaymentMethodsApi
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson
import jakarta.inject.Inject
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.core.Response
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CompletionStage
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

class PaymentMethodsHandlerResource
@Inject
constructor(private val paymentMethodService: PaymentMethodService) : PaymentMethodsApi {

    @ServerExceptionMapper
    fun mapException(exception: PaymentMethodsClientException): RestResponse<ProblemJson> {
        val problem = ProblemJson()
        problem.status = Response.Status.INTERNAL_SERVER_ERROR.statusCode
        problem.title = "Unexpected Exception"
        problem.detail = "Error during GMP communication"
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, problem)
    }

    override fun getAllPaymentMethods(
        xClientId: @NotNull String,
        amount: BigDecimal,
    ): CompletionStage<PaymentMethodsResponse> {
        return paymentMethodService.searchPaymentMethods(
            amount,
            xClientId,
            UUID.randomUUID().toString(),
        )
    }
}
