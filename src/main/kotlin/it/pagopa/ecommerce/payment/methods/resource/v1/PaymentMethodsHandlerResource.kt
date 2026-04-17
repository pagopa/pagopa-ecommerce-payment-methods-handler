package it.pagopa.ecommerce.payment.methods.resource.v1

import it.pagopa.ecommerce.payment.methods.exception.NoBundleFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodNotFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.services.PaymentMethodService
import it.pagopa.ecommerce.payment.methods.v1.server.api.PaymentMethodsApi
import it.pagopa.ecommerce.payment.methods.v1.server.model.CalculateFeeRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.CalculateFeeResponse
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

    override fun calculateFees(
        paymentMethodId: String,
        xClientId: @NotNull String,
        xLanguage: @NotNull String,
        calculateFeeRequest: @Valid @NotNull CalculateFeeRequest,
        maxOccurrences: Int?,
    ): CompletionStage<CalculateFeeResponse> {
        log.info(
            "[Payment Method] Retrieve bundles for client [{}] list for payment method: [{}], allCcp: [{}], isMulti: [{}] and payment notice amounts: {}",
            xClientId,
            paymentMethodId,
            calculateFeeRequest.isAllCCP,
            calculateFeeRequest.paymentNotices.size > 1,
            calculateFeeRequest.paymentNotices
                .stream()
                .map { paymentNotice -> paymentNotice.paymentAmount }
                .toList(),
        )
        val xRequestId = UUID.randomUUID().toString()
        return paymentMethodService.calculateFees(
            paymentMethodId,
            calculateFeeRequest,
            xRequestId,
            xClientId,
            xLanguage,
            maxOccurrences ?: Int.MAX_VALUE,
        )
    }

    override fun getAllPaymentMethods(
        paymentMethodsRequest: @Valid @NotNull PaymentMethodsRequest
    ): CompletionStage<PaymentMethodsResponse> {
        val xRequestId = UUID.randomUUID().toString()
        return paymentMethodService.searchPaymentMethods(paymentMethodsRequest, xRequestId)
    }

    override fun getPaymentMethod(
        id: String,
        xClientId: @NotNull String,
    ): CompletionStage<PaymentMethodResponse> {
        val xRequestId = UUID.randomUUID().toString()
        return paymentMethodService.getPaymentMethod(id, xRequestId, xClientId)
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

    @ServerExceptionMapper
    fun mapMethodNotFoundException(
        exception: PaymentMethodNotFoundException
    ): RestResponse<ProblemJson> {
        return problemResponse(
            Response.Status.NOT_FOUND,
            "Not Found",
            "The requested payment method does not exist or could not be found.",
        )
    }

    @ServerExceptionMapper
    fun mapNoBundleFoundException(exception: NoBundleFoundException): RestResponse<ProblemJson> {
        return problemResponse(
            Response.Status.NOT_FOUND,
            "Not Found",
            "No bundle found for the requested payment method.",
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
