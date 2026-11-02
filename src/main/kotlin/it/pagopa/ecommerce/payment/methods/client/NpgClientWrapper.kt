package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.NpgResponseException
import it.pagopa.ecommerce.payment.methods.utils.LogTracingUtils
import it.pagopa.ecommerce.payment.methods.v1.server.model.NpgBuildFormParams
import it.pagopa.generated.npg.client.api.PaymentServicesApi
import it.pagopa.generated.npg.client.model.ActionTypeDto
import it.pagopa.generated.npg.client.model.CardDataResponseDto
import it.pagopa.generated.npg.client.model.CreateHostedOrderRequestDto
import it.pagopa.generated.npg.client.model.FieldsDto
import it.pagopa.generated.npg.client.model.OrderDto
import it.pagopa.generated.npg.client.model.PaymentSessionDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.UUID
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class NpgClientWrapper
@Inject
constructor(
    @RestClient private val npgRestClient: PaymentServicesApi,
    @ConfigProperty(name = "npg.client.api-key") private val npgDefaultApiKey: String,
) {
    private val log = LoggerFactory.getLogger(NpgClientWrapper::class.java)

    private val langMap =
        mapOf("it" to "ITA", "fr" to "FRA", "de" to "DEU", "sl" to "SLV", "en" to "ENG")
    private val defaultLanguage = "ITA"

    fun buildForm(params: NpgBuildFormParams): Uni<FieldsDto> {

        val npgLanguage =
            params.language?.let { langMap.getOrDefault(it, defaultLanguage) } ?: defaultLanguage

        val request =
            CreateHostedOrderRequestDto().apply {
                version = "2"
                merchantUrl = params.urls.merchantUrl.toString()
                order =
                    OrderDto().apply {
                        orderId = params.orderId
                        amount = "1"
                        currency = "EUR"
                    }
                paymentSession =
                    PaymentSessionDto().apply {
                        actionType = ActionTypeDto.PAY
                        amount = "1"
                        language = npgLanguage
                        paymentService = params.paymentMethod
                        resultUrl = params.urls.resultUrl.toString()
                        cancelUrl = params.urls.cancelUrl.toString()
                        notificationUrl = params.urls.notificationUrl.toString()
                    }
            }

        return npgRestClient
            .pspApiV1OrdersBuildPost(params.correlationId, npgDefaultApiKey, request)
            .map { response ->
                if (response.sessionId == null) {
                    throw NpgResponseException("Missing sessionId in NPG response")
                }
                if (response.securityToken == null) {
                    throw NpgResponseException("Missing securityToken in NPG response")
                }
                response
            }
            .onItem()
            .invoke { _ ->
                LogTracingUtils.loggerTracingUtils()
                    .success()
                    .details(
                        mapOf("orderId" to params.orderId, "paymentMethod" to params.paymentMethod)
                    )
                    .dependency(LogTracingUtils.NPG_DEPENDENCY)
                    .logInfo(log, "NPG buildForm called successfully!")
            }
            .onFailure()
            .invoke { e ->
                LogTracingUtils.loggerTracingUtils()
                    .failure()
                    .details(
                        mapOf("orderId" to params.orderId, "paymentMethod" to params.paymentMethod)
                    )
                    .dependency(LogTracingUtils.NPG_DEPENDENCY)
                    .logWithStackTrace(log, e, "Error calling NPG buildForm")
            }
            .onFailure()
            .transform { e ->
                if (e is NpgResponseException) {
                    e
                } else {
                    NpgResponseException(
                        "Error during NPG buildForm for orderId=${params.orderId}: ${e.message}"
                    )
                }
            }
    }

    fun getCardData(correlationId: UUID, sessionId: String): Uni<CardDataResponseDto> {
        return npgRestClient
            .pspApiV1BuildCardDataGet(correlationId, sessionId, npgDefaultApiKey)
            .onItem()
            .invoke { _ ->
                LogTracingUtils.loggerTracingUtils()
                    .success()
                    .details(mapOf("sessionId" to sessionId))
                    .dependency(LogTracingUtils.NPG_DEPENDENCY)
                    .logInfo(log, "NPG getCardData called successfully!")
            }
            .onFailure()
            .invoke { e ->
                LogTracingUtils.loggerTracingUtils()
                    .failure()
                    .details(mapOf("sessionId" to sessionId))
                    .dependency(LogTracingUtils.NPG_DEPENDENCY)
                    .logWithStackTrace(log, e, "Error calling NPG getCardData")
            }
            .onFailure()
            .transform { e ->
                if (e is NpgResponseException) {
                    e
                } else {
                    NpgResponseException(
                        "Error during NPG getCardData for sessionId=$sessionId: ${e.message}"
                    )
                }
            }
    }
}
