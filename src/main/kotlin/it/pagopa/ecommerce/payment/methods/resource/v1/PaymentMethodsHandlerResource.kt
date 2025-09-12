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
import org.slf4j.LoggerFactory

class PaymentMethodsHandlerResource
@Inject
constructor(private val paymentMethodService: PaymentMethodService) : PaymentMethodsApi {
    private val log = LoggerFactory.getLogger(PaymentMethodsHandlerResource::class.java)

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

    @ServerExceptionMapper
    fun mapPaymentMethodsClientException(exception: PaymentMethodsClientException) =
        problemResponse(
            Response.Status.INTERNAL_SERVER_ERROR,
            "Unexpected Exception",
            "Error during GMP communication",
        )

    @ServerExceptionMapper
    fun mapException(exception: Exception): RestResponse<ProblemJson> {
        log.error("Generic Exception While Processing the Request", exception)
        return problemResponse(
            Response.Status.INTERNAL_SERVER_ERROR,
            "Unexpected Exception",
            "Generic Error",
        )
    }

    private fun problemResponse(
        status: Response.Status,
        title: String,
        detail: String,
    ): RestResponse<ProblemJson> {
        val problem =
            ProblemJson().apply {
                this.status = status.statusCode
                this.title = title
                this.detail = detail
            }
        return RestResponse.status(status, problem)
    }
}
