package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "npg-api")
@Suppress("kotlin:S6517")
interface NpgRestClient {

    @POST
    @Path("/psp/api/v1/orders/build")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun buildForm(
        @HeaderParam("Correlation-Id") correlationId: String,
        @HeaderParam("apikey") apiKey: String,
        request: NpgBuildRequest,
    ): Uni<NpgBuildResponse>

    @GET
    @Path("/psp/api/v1/build/cardData")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCardData(
        @HeaderParam("Correlation-Id") correlationId: String,
        @HeaderParam("X-API-KEY") apiKey: String,
        @QueryParam("sessionId") sessionId: String,
    ): Uni<NpgCardDataResponse>
}
