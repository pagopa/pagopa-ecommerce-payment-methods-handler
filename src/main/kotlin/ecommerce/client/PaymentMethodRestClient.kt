package ecommerce.client

import io.smallrye.mutiny.Uni
import ecommerce.config.JwtHeaderFactory
import ecommerce.dto.PaymentMethod
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.util.concurrent.CompletionStage

@Path("/payment-method")
@RegisterRestClient(configKey = "payment-method")
@RegisterClientHeaders(JwtHeaderFactory::class)
interface PaymentMethodRestClient {

    @GET
    fun getAll(): Set<PaymentMethod>

    @GET
    fun getAllAsync(): CompletionStage<PaymentMethod>

    @GET
    fun getAllAsUni(): Uni<PaymentMethod>

    @GET
    fun getById(@QueryParam("id") id: String): Set<PaymentMethod>

    @GET
    fun getByIdAsync(@QueryParam("id") id: String): CompletionStage<Set<PaymentMethod>>

    @GET
    fun getByIdAsUni(@QueryParam("id") id: String): Uni<Set<PaymentMethod>>
}
