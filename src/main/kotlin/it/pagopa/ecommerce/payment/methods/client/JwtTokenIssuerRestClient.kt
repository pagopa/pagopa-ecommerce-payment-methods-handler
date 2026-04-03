package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

data class CreateTokenRequest(
    val privateClaims: Map<String, String>,
    val audience: String,
    val duration: Int,
)

data class CreateTokenResponse(val token: String)

@RegisterRestClient(configKey = "jwt-issuer-api")
fun interface JwtTokenIssuerRestClient {

    @POST
    @Path("/tokens")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createJwtToken(
        @HeaderParam("x-api-key") apiKey: String,
        request: CreateTokenRequest,
    ): Uni<CreateTokenResponse>
}
