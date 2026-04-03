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

@ApplicationScoped
class NpgClientWrapper
@Inject
constructor(
    @RestClient private val npgRestClient: NpgRestClient,
    @ConfigProperty(name = "npg.client.api-key") private val npgDefaultApiKey: String,
) {
    private val log = LoggerFactory.getLogger(NpgClientWrapper::class.java)

    fun buildForm(
        correlationId: UUID,
        merchantUrl: URI,
        resultUrl: URI,
        notificationUrl: URI,
        cancelUrl: URI,
        orderId: String,
        paymentMethod: NpgPaymentMethod,
        language: String?,
    ): Uni<NpgFieldsDto> {
        log.info(
            "Calling NPG buildForm with correlationId={}, orderId={}, paymentMethod={}",
            correlationId,
            orderId,
            paymentMethod.serviceName,
        )

        val request =
            NpgBuildRequest(
                merchantUrl = merchantUrl.toString(),
                resultUrl = resultUrl.toString(),
                notificationUrl = notificationUrl.toString(),
                cancelUrl = cancelUrl.toString(),
                orderId = orderId,
                paymentService = paymentMethod.serviceName,
                language = language,
            )

        return npgRestClient
            .buildForm(
                correlationId = correlationId.toString(),
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
            .invoke { e -> log.error("Error calling NPG buildForm for orderId=$orderId", e) }
    }
}
