package ecommerce.controller

import io.smallrye.mutiny.Uni
import ecommerce.dto.PaymentMethod
import ecommerce.services.PaymentMethodService
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletionStage

@Path("/payment-method")
class PaymentMethodController {

    @Inject
    lateinit var paymentMethodService: PaymentMethodService

    private val logger: Logger = LoggerFactory.getLogger(PaymentMethodController::class.java)


    @GET
    @Path("/all")
    fun getAllPaymentMethods(): Set<PaymentMethod> {
        logger.info("CALL GET ALL PAYMENT-METHOD")
        return paymentMethodService.getAll()
    }

    @GET
    @Path("/all-async")
    fun getAllPaymentMethodsAsync(): CompletionStage<PaymentMethod> {
        return paymentMethodService.getAllAsync()
    }

    @GET
    @Path("/all-as-uni")
    fun getAllPaymentMethodsAsUni(): Uni<PaymentMethod> {
        return paymentMethodService.getAllAsUni()
    }

    @GET
    @Path("/id/{id}")
    fun getPaymentMethod(@PathParam("id") id: String): Set<PaymentMethod> {
        return paymentMethodService.getById(id)
    }

    @GET
    @Path("/id-async/{id}")
    fun getPaymentMethodAsync(@PathParam("id") id: String): CompletionStage<Set<PaymentMethod>> {
        return paymentMethodService.getByIdAsync(id)
    }

    @GET
    @Path("/id-uni/{id}")
    fun getPaymentMethodMutiny(@PathParam("id") id: String): Uni<Set<PaymentMethod>> {
        return paymentMethodService.getByIdAsUni(id)
    }
}
