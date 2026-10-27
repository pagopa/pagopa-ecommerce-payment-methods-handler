package it.pagopa.ecommerce.payment.methods.resource.v1

import com.fasterxml.jackson.core.JsonProcessingException
import it.pagopa.ecommerce.payment.methods.exception.JwtIssuerResponseException
import it.pagopa.ecommerce.payment.methods.exception.NoBundleFoundException
import it.pagopa.ecommerce.payment.methods.exception.NpgResponseException
import it.pagopa.ecommerce.payment.methods.exception.OrderIdNotFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodNotFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.exception.SessionAlreadyAssociatedToTransactionException
import it.pagopa.ecommerce.payment.methods.exception.UniqueIdGenerationException
import it.pagopa.ecommerce.payment.methods.services.PaymentMethodService
import it.pagopa.ecommerce.payment.methods.utils.LogTracingUtils
import it.pagopa.ecommerce.payment.methods.v1.server.api.PaymentMethodsApi
import it.pagopa.ecommerce.payment.methods.v1.server.model.CalculateFeeRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.CalculateFeeResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.CreateSessionResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PatchSessionRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson
import it.pagopa.ecommerce.payment.methods.v1.server.model.SessionPaymentMethodResponse
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.validation.ValidationException
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.NotAuthorizedException
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

    companion object {
        private const val NOT_FOUND_TITLE = "Not Found"
    }

    override fun calculateFees(
        paymentMethodId: String,
        xClientId: @NotNull String,
        xLanguage: @NotNull String,
        calculateFeeRequest: @Valid @NotNull CalculateFeeRequest,
        maxOccurrences: Int?,
    ): CompletionStage<CalculateFeeResponse> {
        //        log.info(
        //            "[Payment Method] Retrieve bundles for client [{}] list for payment method:
        // [{}], allCcp: [{}], isMulti: [{}] and payment notice amounts: {}",
        //            xClientId,
        //            paymentMethodId,
        //            calculateFeeRequest.isAllCCP,
        //            calculateFeeRequest.paymentNotices.size > 1,
        //            calculateFeeRequest.paymentNotices
        //                .stream()
        //                .map { paymentNotice -> paymentNotice.paymentAmount }
        //                .toList(),
        //        )
        val xRequestId = UUID.randomUUID().toString()
        // Add the details to the context for logging purposes, so that it can be propagated to
        // downstream services and included in log messages..
        LogTracingUtils.enrichContext(
            LogTracingUtils.Companion.AttributeKeys.CTX_REQUEST_ID,
            xRequestId,
        )
        LogTracingUtils.enrichContext(
            LogTracingUtils.Companion.AttributeKeys.CTX_CLIENT_ID,
            xClientId,
        )

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
        @Valid @NotNull paymentMethodsRequest: PaymentMethodsRequest
    ): CompletionStage<PaymentMethodsResponse> {
        val xRequestId = UUID.randomUUID().toString()
        // Add the details to the context for logging purposes, so that it can be propagated to
        // downstream services and included in log messages..
        LogTracingUtils.enrichContext(
            mapOf(
                LogTracingUtils.Companion.AttributeKeys.CTX_REQUEST_ID to xRequestId,
                LogTracingUtils.Companion.AttributeKeys.EVENT_ACTION to "getAllPaymentMethods",
            )
        )
        return paymentMethodService.searchPaymentMethods(paymentMethodsRequest, xRequestId)
    }

    override fun getPaymentMethod(
        id: String,
        xClientId: it.pagopa.ecommerce.payment.methods.v1.server.model.ClientId?,
    ): CompletionStage<PaymentMethodResponse> {
        val xRequestId = UUID.randomUUID().toString()
        // Add the details to the context for logging purposes, so that it can be propagated to
        // downstream services and included in log messages..
        LogTracingUtils.enrichContext(
            mapOf(
                LogTracingUtils.Companion.AttributeKeys.CTX_REQUEST_ID to xRequestId,
                LogTracingUtils.Companion.AttributeKeys.EVENT_ACTION to "getPaymentMethod",
                LogTracingUtils.Companion.AttributeKeys.CTX_CLIENT_ID to xClientId.toString(),
            )
        )
        return paymentMethodService.getPaymentMethod(id, xRequestId, xClientId?.toString())
    }

    override fun createSession(
        id: String,
        lang: String?,
        xClientId: it.pagopa.ecommerce.payment.methods.v1.server.model.ClientId?,
    ): CompletionStage<CreateSessionResponse> {
        val xRequestId = UUID.randomUUID().toString()
        // Add the details to the context for logging purposes, so that it can be propagated to
        // downstream services and included in log messages..
        LogTracingUtils.enrichContext(
            mapOf(
                LogTracingUtils.Companion.AttributeKeys.CTX_REQUEST_ID to xRequestId,
                LogTracingUtils.Companion.AttributeKeys.EVENT_ACTION to "createSession",
                LogTracingUtils.Companion.AttributeKeys.CTX_CLIENT_ID to xClientId.toString(),
            )
        )
        return paymentMethodService
            .createSessionForPaymentMethod(id, lang, xClientId?.toString())
            .subscribeAsCompletionStage()
    }

    override fun getSessionPaymentMethod(
        id: String,
        orderId: String,
        xClientId: it.pagopa.ecommerce.payment.methods.v1.server.model.ClientId?,
    ): CompletionStage<SessionPaymentMethodResponse> {
        // Add the details to the context for logging purposes, so that it can be propagated to
        // downstream services and included in log messages
        LogTracingUtils.enrichContext(
            mapOf(
                LogTracingUtils.Companion.AttributeKeys.EVENT_ACTION to "getSessionPaymentMethod",
                LogTracingUtils.Companion.AttributeKeys.CTX_CLIENT_ID to xClientId.toString(),
            )
        )
        return paymentMethodService
            .getCardDataInformation(id, orderId, xClientId?.toString())
            .subscribeAsCompletionStage()
    }

    override fun updateSession(
        id: String,
        orderId: String,
        patchSessionRequest: @Valid @NotNull PatchSessionRequest,
        xClientId: it.pagopa.ecommerce.payment.methods.v1.server.model.ClientId?,
    ): CompletionStage<Void> {
        val xRequestId = UUID.randomUUID().toString()
        // Add the details to the context for logging purposes, so that it can be propagated to
        // downstream services and included in log messages
        LogTracingUtils.enrichContext(
            mapOf(
                LogTracingUtils.Companion.AttributeKeys.CTX_REQUEST_ID to xRequestId,
                LogTracingUtils.Companion.AttributeKeys.EVENT_ACTION to "updateSession",
                LogTracingUtils.Companion.AttributeKeys.CTX_CLIENT_ID to xClientId.toString(),
            )
        )
        return paymentMethodService
            .updateSession(id, orderId, patchSessionRequest, xClientId?.toString())
            .subscribeAsCompletionStage()
    }

    override fun getTransactionIdForSession(
        id: String,
        orderId: String,
        authorization: String,
        xClientId: it.pagopa.ecommerce.payment.methods.v1.server.model.ClientId?,
    ): CompletionStage<
        it.pagopa.ecommerce.payment.methods.v1.server.model.SessionGetTransactionIdResponse
    > {
        // Add the details to the context for logging purposes, so that it can be propagated to
        // downstream services and included in log messages
        LogTracingUtils.enrichContext(
            mapOf(
                LogTracingUtils.Companion.AttributeKeys.EVENT_ACTION to
                    "getTransactionIdForSession",
                LogTracingUtils.Companion.AttributeKeys.CTX_CLIENT_ID to xClientId.toString(),
            )
        )

        val securityToken =
            authorization
                .takeIf { it.startsWith("Bearer ") }
                ?.removePrefix("Bearer ")
                ?.trim()
                ?.takeIf { it.isNotEmpty() } ?: throw NotAuthorizedException("Bearer")

        return paymentMethodService
            .getTransactionIdForSession(id, orderId, securityToken, xClientId?.toString())
            .map { transactionId ->
                it.pagopa.ecommerce.payment.methods.v1.server.model
                    .SessionGetTransactionIdResponse()
                    .apply {
                        this.transactionId = transactionId
                        this.base64TransactionId = uuidToBase64(UUID.fromString(transactionId))
                    }
            }
            .subscribeAsCompletionStage()
    }

    /**
     * Encodes a UUID into a shorter, reversible Base64 URL-safe string, encoding the 16 raw bytes
     * of the UUID. This mirrors the conversion performed by the ecommerce-commons library so that
     * the value stays consistent across services.
     */
    private fun uuidToBase64(uuid: UUID): String {
        val buffer = java.nio.ByteBuffer.wrap(ByteArray(16))
        buffer.putLong(uuid.mostSignificantBits)
        buffer.putLong(uuid.leastSignificantBits)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array())
    }

    @ServerExceptionMapper
    fun mapNotAuthorizedException(exception: NotAuthorizedException): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Unauthorized Request: Missing or malformed Bearer token")
        }

        return problemResponse(
            Response.Status.UNAUTHORIZED,
            "Unauthorized",
            "Missing or invalid Authorization Bearer token",
        )
    }

    @ServerExceptionMapper
    fun mapNpgResponseException(exception: NpgResponseException): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "NPG Response Exception")
        }
        return problemResponse(
            Response.Status.BAD_GATEWAY,
            "Bad Gateway",
            "Error communicating with the payment gateway",
        )
    }

    @ServerExceptionMapper
    fun mapJwtIssuerResponseException(
        exception: JwtIssuerResponseException
    ): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "JWT Issuer Response Exception")
        }
        return problemResponse(
            Response.Status.BAD_GATEWAY,
            "Bad Gateway",
            "Error creating notification token",
        )
    }

    @ServerExceptionMapper
    fun mapUniqueIdGenerationException(
        exception: UniqueIdGenerationException
    ): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Unique ID Generation Exception")
        }
        return problemResponse(
            Response.Status.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            exception.message.orEmpty(),
        )
    }

    @ServerExceptionMapper
    fun mapPaymentMethodsClientException(
        exception: PaymentMethodsClientException
    ): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Payment Methods Client Exception")
        }
        return problemResponse(
            Response.Status.INTERNAL_SERVER_ERROR,
            "Unexpected Exception",
            "Error during GMP communication",
        )
    }

    @ServerExceptionMapper
    fun mapException(exception: Exception): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Generic Exception While Processing the Request")
        }
        return problemResponse(
            Response.Status.INTERNAL_SERVER_ERROR,
            "Unexpected Exception",
            "Generic Error",
        )
    }

    @ServerExceptionMapper
    fun mapValidationException(exception: ValidationException): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Validation Exception While Processing the Request")
        }
        val detail =
            if (exception is jakarta.validation.ConstraintViolationException) {
                exception.constraintViolations.joinToString("; ") {
                    "${it.propertyPath}: ${it.message}"
                }
            } else {
                "The request is malformed, contains invalid parameters, or is missing required information."
            }
        return problemResponse(Response.Status.BAD_REQUEST, "Bad Request", detail)
    }

    @ServerExceptionMapper
    fun mapJsonProcessingException(exception: JsonProcessingException): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Malformed request body")
        }
        return problemResponse(
            Response.Status.BAD_REQUEST,
            "Bad Request",
            "The request body is malformed or is missing required information.",
        )
    }

    @ServerExceptionMapper
    fun mapMethodNotFoundException(
        exception: PaymentMethodNotFoundException
    ): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Payment Method Not Found")
        }
        return problemResponse(
            Response.Status.NOT_FOUND,
            NOT_FOUND_TITLE,
            "The requested payment method does not exist or could not be found.",
        )
    }

    @ServerExceptionMapper
    fun mapOrderIdNotFoundException(
        exception: OrderIdNotFoundException
    ): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Order ID Not Found")
        }
        return problemResponse(
            Response.Status.NOT_FOUND,
            NOT_FOUND_TITLE,
            exception.message.orEmpty(),
        )
    }

    @ServerExceptionMapper
    fun mapNoBundleFoundException(exception: NoBundleFoundException): RestResponse<ProblemJson> {
        return problemResponse(
            Response.Status.NOT_FOUND,
            NOT_FOUND_TITLE,
            "No bundle found for the requested payment method.",
        )
    }

    @ServerExceptionMapper
    fun mapSessionAlreadyAssociatedToTransactionException(
        exception: SessionAlreadyAssociatedToTransactionException
    ): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Session Already Associated To Transaction")
        }
        return problemResponse(
            Response.Status.CONFLICT,
            "Session already associated to transaction",
            exception.message.orEmpty(),
        )
    }

    @ServerExceptionMapper
    fun mapInvalidSessionException(
        exception: it.pagopa.ecommerce.payment.methods.exception.InvalidSessionException
    ): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Invalid Session")
        }
        return problemResponse(
            Response.Status.CONFLICT,
            "Invalid session",
            exception.message.orEmpty(),
        )
    }

    @ServerExceptionMapper
    fun mapMismatchedSecurityTokenException(
        exception: it.pagopa.ecommerce.payment.methods.exception.MismatchedSecurityTokenException
    ): RestResponse<ProblemJson> {
        if (log.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .details(mapOf("exception" to exception.message.orEmpty()))
                .logDebug(log, "Mismatched Security Token")
        }
        return problemResponse(
            Response.Status.CONFLICT,
            "Conflict",
            "Invalid security token for the requested order id",
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
