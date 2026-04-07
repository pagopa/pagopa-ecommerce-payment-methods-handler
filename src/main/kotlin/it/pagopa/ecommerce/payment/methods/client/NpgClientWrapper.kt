package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.NpgResponseException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.net.URI
import java.util.UUID
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

data class NpgBuildFormParams(
    val correlationId: UUID,
    val urls: NpgSessionUrls,
    val orderId: String,
    val paymentMethod: NpgPaymentMethod,
    val language: String?,
)

data class NpgSessionUrls(
    val merchantUrl: URI,
    val resultUrl: URI,
    val notificationUrl: URI,
    val cancelUrl: URI,
)

@ApplicationScoped
class NpgClientWrapper
@Inject
constructor(
    @RestClient private val npgRestClient: NpgRestClient,
    @ConfigProperty(name = "npg.client.api-key") private val npgDefaultApiKey: String,
) {
    private val log = LoggerFactory.getLogger(NpgClientWrapper::class.java)

    fun buildForm(params: NpgBuildFormParams): Uni<NpgFieldsDto> {
        log.info(
            "Calling NPG buildForm with correlationId={}, orderId={}, paymentMethod={}",
            params.correlationId,
            params.orderId,
            params.paymentMethod.serviceName,
        )

        val request =
            NpgBuildRequest(
                merchantUrl = params.urls.merchantUrl.toString(),
                resultUrl = params.urls.resultUrl.toString(),
                notificationUrl = params.urls.notificationUrl.toString(),
                cancelUrl = params.urls.cancelUrl.toString(),
                orderId = params.orderId,
                paymentService = params.paymentMethod.serviceName,
                language = params.language,
            )

        return npgRestClient
            .buildForm(
                correlationId = params.correlationId.toString(),
                apiKey = "Bearer $npgDefaultApiKey",
                request = request,
            )
            .map { response ->
                NpgFieldsDto(
                    sessionId =
                        response.sessionId
                            ?: throw NpgResponseException("Missing sessionId in NPG response"),
                    securityToken =
                        response.securityToken
                            ?: throw NpgResponseException("Missing securityToken in NPG response"),
                    fields =
                        response.fields?.map { field ->
                            NpgFieldDto(
                                id = field.id,
                                type = field.type,
                                propertyClass = field.propertyClass,
                                src = field.src,
                            )
                        } ?: emptyList(),
                )
            }
            .onFailure()
            .invoke { e ->
                log.error("Error calling NPG buildForm for orderId=${params.orderId}", e)
            }
    }
}
