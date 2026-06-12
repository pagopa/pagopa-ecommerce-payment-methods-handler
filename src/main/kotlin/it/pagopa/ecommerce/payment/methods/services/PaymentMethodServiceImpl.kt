package it.pagopa.ecommerce.payment.methods.services

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.client.CreateTokenRequest
import it.pagopa.ecommerce.payment.methods.client.JwtTokenIssuerClient
import it.pagopa.ecommerce.payment.methods.client.NpgClientWrapper
import it.pagopa.ecommerce.payment.methods.client.NpgPaymentMethod
import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.ecommerce.payment.methods.config.SessionUrlConfig
import it.pagopa.ecommerce.payment.methods.domain.CardDataDocument
import it.pagopa.ecommerce.payment.methods.domain.NpgSessionDocument
import it.pagopa.ecommerce.payment.methods.exception.OrderIdNotFoundException
import it.pagopa.ecommerce.payment.methods.exception.SessionAlreadyAssociatedToTransactionException
import it.pagopa.ecommerce.payment.methods.infrastructure.NpgSessionsRedisWrapper
import it.pagopa.ecommerce.payment.methods.mappers.toPaymentMethodRequestDto
import it.pagopa.ecommerce.payment.methods.mappers.toPaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.mappers.toPaymentMethodsResponse
import it.pagopa.ecommerce.payment.methods.utils.UniqueIdGenerator
import it.pagopa.ecommerce.payment.methods.v1.server.model.CardFormFields
import it.pagopa.ecommerce.payment.methods.v1.server.model.CreateSessionResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.Field
import it.pagopa.ecommerce.payment.methods.v1.server.model.NpgBuildFormParams
import it.pagopa.ecommerce.payment.methods.v1.server.model.NpgSessionUrls
import it.pagopa.ecommerce.payment.methods.v1.server.model.PatchSessionRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.SessionPaymentMethodResponse
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.net.URI
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletionStage
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory

@ApplicationScoped
class PaymentMethodServiceImpl
@Inject
constructor(
    private val restClient: PaymentMethodsClient,
    private val npgClient: NpgClientWrapper,
    private val jwtTokenIssuerClient: JwtTokenIssuerClient,
    private val npgSessionsRedisWrapper: NpgSessionsRedisWrapper,
    private val uniqueIdGenerator: UniqueIdGenerator,
    private val sessionUrlConfig: SessionUrlConfig,
    @ConfigProperty(name = "npg.notification.jwt.validity.time")
    private val npgNotificationTokenValidityTime: Int,
) : PaymentMethodService {

    private val log = LoggerFactory.getLogger(PaymentMethodServiceImpl::class.java)

    override fun searchPaymentMethods(
        paymentMethodsRequest: PaymentMethodsRequest,
        xRequestId: String,
    ): CompletionStage<PaymentMethodsResponse> {
        return restClient
            .searchPaymentMethods(paymentMethodsRequest.toPaymentMethodRequestDto(), xRequestId)
            .map { dto -> dto.toPaymentMethodsResponse() }
            .map { dto -> filterMethods(paymentMethodsRequest, dto) }
            .onFailure()
            .invoke { exception ->
                log.error("Exception during request with id $xRequestId", exception)
            }
            .onItem()
            .invoke { _ ->
                log.info("Payment methods retrieved successfully for request with id $xRequestId")
            }
            .subscribeAsCompletionStage()
    }

    fun filterMethods(
        paymentMethodRequest: PaymentMethodsRequest,
        paymentMethodsResponse: PaymentMethodsResponse,
    ): PaymentMethodsResponse {
        val deviceVersion = paymentMethodRequest.deviceVersion
        val userTouchpoint = paymentMethodRequest.userTouchpoint
        return when (userTouchpoint) {
            /*
            payment methods filtering logic:
            we should not return CARDS payment method with ONBOARDABLE for old app version that will not handle this payment method
            Actually we identify IO app using user touchpoint field
            and old app version based on the fact that deviceVersion parameter is not valued.
            For those versions method management for card method is overridden to ONBOARDABLE_ONLY
            */
            PaymentMethodsRequest.UserTouchpointEnum.IO ->
                if (deviceVersion == null) {
                    paymentMethodsResponse.paymentMethods(
                        paymentMethodsResponse.paymentMethods.map {
                            if (it.paymentTypeCode == "CP") {
                                it.methodManagement =
                                    PaymentMethodResponse.MethodManagementEnum.ONBOARDABLE_ONLY
                            }
                            it
                        }
                    )
                } else {
                    paymentMethodsResponse
                }

            else -> paymentMethodsResponse
        }
    }

    override fun getPaymentMethod(
        paymentMethodsId: String,
        xRequestId: String,
        xClientId: String,
    ): CompletionStage<PaymentMethodResponse> {
        return restClient
            .getPaymentMethod(paymentMethodsId, xRequestId, xClientId)
            .map { dto -> dto.toPaymentMethodResponse() }
            .onFailure()
            .invoke { exception ->
                log.error(
                    "Exception during request with id $xRequestId and client id $xClientId",
                    exception,
                )
            }
            .onItem()
            .invoke { _ ->
                log.info(
                    "Payment method retrieved successfully for request with id $xRequestId and client id $xClientId"
                )
            }
            .subscribeAsCompletionStage()
    }

    override fun createSessionForPaymentMethod(
        paymentMethodId: String,
        language: String?,
        xClientId: String,
    ): Uni<CreateSessionResponse> {
        log.info(
            "[Payment Method handler] create new NPG session using paymentMethodId: {}",
            paymentMethodId,
        )

        val xRequestId = UUID.randomUUID().toString()

        return restClient
            .getPaymentMethod(paymentMethodId, xRequestId, xClientId)
            .map { response -> NpgPaymentMethod.fromPaymentTypeCode(response.group) }
            .flatMap { paymentMethod ->
                uniqueIdGenerator.generateUniqueId().map { orderId -> Pair(orderId, paymentMethod) }
            }
            .flatMap { (orderId, paymentMethod) ->
                jwtTokenIssuerClient
                    .createJWTToken(
                        CreateTokenRequest(
                            privateClaims =
                                mapOf(
                                    JwtTokenIssuerClient.ORDER_ID_CLAIM to orderId,
                                    JwtTokenIssuerClient.PAYMENT_METHOD_ID_CLAIM to paymentMethodId,
                                ),
                            audience = JwtTokenIssuerClient.NPG_AUDIENCE,
                            duration = npgNotificationTokenValidityTime,
                        )
                    )
                    .map { tokenResponse -> Triple(orderId, paymentMethod, tokenResponse.token) }
            }
            .flatMap { (orderId, paymentMethod, notificationToken) ->
                val correlationId = UUID.randomUUID()
                log.info("Generated correlationId for NPG build session: {}", correlationId)

                val isIoClient = xClientId.equals("IO", ignoreCase = true)
                val returnUrlBasePath =
                    if (isIoClient) sessionUrlConfig.ioBasePath() else sessionUrlConfig.basePath()

                val resultUrl =
                    buildSessionOutcomeUrl(returnUrlBasePath, sessionUrlConfig.outcomeSuffix())
                val cancelUrl =
                    buildSessionOutcomeUrl(returnUrlBasePath, sessionUrlConfig.cancelSuffix())
                val notificationUrl =
                    buildNotificationUrl(
                        sessionUrlConfig.notificationUrl(),
                        orderId,
                        notificationToken,
                    )

                npgClient
                    .buildForm(
                        NpgBuildFormParams().apply {
                            this.correlationId = correlationId
                            this.urls =
                                NpgSessionUrls().apply {
                                    this.merchantUrl = returnUrlBasePath
                                    this.resultUrl = resultUrl
                                    this.notificationUrl = notificationUrl
                                    this.cancelUrl = cancelUrl
                                }
                            this.orderId = orderId
                            this.paymentMethod = paymentMethod.serviceName
                            this.language = language
                        }
                    )
                    .map { fields ->
                        SessionBuildData(fields, paymentMethod, orderId, correlationId)
                    }
            }
            .flatMap { data ->
                npgSessionsRedisWrapper
                    .save(
                        NpgSessionDocument(
                            orderId = data.orderId,
                            correlationId = data.correlationId.toString(),
                            sessionId = data.fields.sessionId,
                            securityToken = data.fields.securityToken,
                        )
                    )
                    .replaceWith(data)
            }
            .map { data ->
                CreateSessionResponse().apply {
                    orderId = data.orderId
                    correlationId = data.correlationId
                    paymentMethodData =
                        CardFormFields().apply {
                            paymentMethod = data.paymentMethod.serviceName
                            form =
                                (data.fields.fields ?: emptyList()).map { field ->
                                    Field().apply {
                                        id = field.id
                                        type = field.type
                                        propertyClass = field.propertyClass
                                        src = field.src?.let { URI.create(it) }
                                    }
                                }
                        }
                }
            }
    }

    override fun getCardDataInformation(
        paymentMethodId: String,
        orderId: String,
        xClientId: String,
    ): Uni<SessionPaymentMethodResponse> {
        log.info(
            "[Payment Method handler] Retrieve card data from NPG using paymentMethodId: {} and orderId: {}",
            paymentMethodId,
            orderId,
        )

        val xRequestId = UUID.randomUUID().toString()

        return restClient
            .getPaymentMethod(paymentMethodId, xRequestId, xClientId)
            .flatMap { npgSessionsRedisWrapper.findById(orderId) }
            .onItem()
            .ifNull()
            .failWith { OrderIdNotFoundException(orderId) }
            .flatMap { session ->
                if (session!!.cardData != null) {
                    log.info("Cache hit for orderId: {}", orderId)
                    Uni.createFrom()
                        .item(
                            SessionPaymentMethodResponse().apply {
                                sessionId = session.sessionId
                                bin = session.cardData.bin
                                lastFourDigits = session.cardData.lastFourDigits
                                expiringDate = session.cardData.expiringDate
                                brand = session.cardData.circuit
                            }
                        )
                } else {
                    log.info("Cache miss for orderId: {}", orderId)
                    val correlationId = UUID.randomUUID()
                    npgClient
                        .getCardData(correlationId, session.sessionId)
                        .flatMap { cardData ->
                            npgSessionsRedisWrapper
                                .save(
                                    NpgSessionDocument(
                                        orderId = session.orderId,
                                        correlationId = session.correlationId,
                                        sessionId = session.sessionId,
                                        securityToken = session.securityToken,
                                        cardData =
                                            CardDataDocument(
                                                bin = cardData.bin ?: "",
                                                lastFourDigits = cardData.lastFourDigits ?: "",
                                                expiringDate = cardData.expiringDate ?: "",
                                                circuit = cardData.circuit ?: "",
                                            ),
                                        transactionId = session.transactionId,
                                    )
                                )
                                .replaceWith(cardData)
                        }
                        .map { cardData ->
                            SessionPaymentMethodResponse().apply {
                                sessionId = session.sessionId
                                bin = cardData.bin
                                lastFourDigits = cardData.lastFourDigits
                                expiringDate = cardData.expiringDate
                                brand = cardData.circuit
                            }
                        }
                }
            }
    }

    override fun updateSession(
        paymentMethodId: String,
        orderId: String,
        patchSessionRequest: PatchSessionRequest,
        xClientId: String,
    ): Uni<Void> {
        log.info(
            "[Payment Method handler] Update session for paymentMethodId: {} and orderId: {}",
            paymentMethodId,
            orderId,
        )

        val xRequestId = UUID.randomUUID().toString()

        return restClient
            .getPaymentMethod(paymentMethodId, xRequestId, xClientId)
            .flatMap { npgSessionsRedisWrapper.findById(orderId) }
            .onItem()
            .ifNull()
            .failWith { OrderIdNotFoundException(orderId) }
            .flatMap { session ->
                val existingTransactionId = session!!.transactionId
                val requestedTransactionId = patchSessionRequest.transactionId

                if (
                    existingTransactionId != null && existingTransactionId != requestedTransactionId
                ) {
                    log.error(
                        "Session's transaction id ({}) differs from requested transaction id ({})",
                        existingTransactionId,
                        requestedTransactionId,
                    )
                    Uni.createFrom()
                        .failure(
                            SessionAlreadyAssociatedToTransactionException(
                                orderId,
                                existingTransactionId,
                                requestedTransactionId,
                            )
                        )
                } else if (existingTransactionId != null) {
                    // Transaction already associated to session (retry case), no-op
                    Uni.createFrom().voidItem()
                } else {
                    // Associate transaction to session
                    val updatedDocument =
                        NpgSessionDocument(
                            orderId = session.orderId,
                            correlationId = session.correlationId,
                            sessionId = session.sessionId,
                            securityToken = session.securityToken,
                            cardData = session.cardData,
                            transactionId = requestedTransactionId,
                        )
                    npgSessionsRedisWrapper.save(updatedDocument).replaceWithVoid()
                }
            }
    }

    private fun buildSessionOutcomeUrl(basePath: URI, suffix: String): URI {
        return URI.create("${basePath}${suffix}?t=${Instant.now().toEpochMilli()}")
    }

    private fun buildNotificationUrl(template: String, orderId: String, sessionToken: String): URI {
        return URI.create(
            template.replace("{orderId}", orderId).replace("{sessionToken}", sessionToken)
        )
    }

    private data class SessionBuildData(
        val fields: it.pagopa.generated.npg.client.model.FieldsDto,
        val paymentMethod: NpgPaymentMethod,
        val orderId: String,
        val correlationId: UUID,
    )
}
