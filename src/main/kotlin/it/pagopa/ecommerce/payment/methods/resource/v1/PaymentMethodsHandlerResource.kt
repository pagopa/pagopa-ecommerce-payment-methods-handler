package it.pagopa.ecommerce.payment.methods.resource.v1

import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.services.PaymentMethodService
import it.pagopa.ecommerce.payment.methods.v1.server.api.PaymentMethodsApi
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.validation.ValidationException
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.core.Response
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
        paymentMethodsRequest: @Valid @NotNull PaymentMethodsRequest
    ): CompletionStage<PaymentMethodsResponse> {
        val xRequestId = UUID.randomUUID().toString()
        return paymentMethodService.searchPaymentMethods(paymentMethodsRequest, xRequestId)
    }

    override fun getPaymentMethod(
        id: String?,
        xClientId: @NotNull String?,
    ): CompletionStage<PaymentMethodResponse?>? {
        TODO("Not yet implemented")
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

    @ServerExceptionMapper
    fun mapValidationException(exception: ValidationException): RestResponse<ProblemJson> {
        log.error("Validation Exception While Processing the Request", exception)
        return problemResponse(
            Response.Status.BAD_REQUEST,
            "Bad Request",
            "The request is malformed, contains invalid parameters, or is missing required information.",
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
