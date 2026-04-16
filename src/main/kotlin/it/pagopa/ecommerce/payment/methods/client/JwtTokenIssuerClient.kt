package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.JwtIssuerResponseException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class JwtTokenIssuerClient
@Inject
constructor(
    @RestClient private val jwtIssuerRestClient: JwtTokenIssuerRestClient,
    @ConfigProperty(name = "jwt-issuer.api-key") private val apiKey: String,
) {
    private val log = LoggerFactory.getLogger(JwtTokenIssuerClient::class.java)

    companion object {
        const val ORDER_ID_CLAIM = "orderId"
        const val PAYMENT_METHOD_ID_CLAIM = "paymentMethodId"
        const val NPG_AUDIENCE = "npg"
    }

    fun createJWTToken(request: CreateTokenRequest): Uni<CreateTokenResponse> {
        return jwtIssuerRestClient
            .createJwtToken(apiKey, request)
            .onFailure()
            .invoke { e -> log.error("Error creating JWT token", e) }
            .onFailure()
            .transform { e ->
                JwtIssuerResponseException("Error while invoking JWT token creation", e)
            }
    }
}
