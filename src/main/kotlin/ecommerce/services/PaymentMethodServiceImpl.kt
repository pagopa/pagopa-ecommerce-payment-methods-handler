package ecommerce.services

import io.smallrye.mutiny.Uni
import ecommerce.client.PaymentMethodRestClient
import ecommerce.dto.PaymentMethod
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.concurrent.CompletionStage

@ApplicationScoped
class PaymentMethodServiceImpl @Inject constructor(
    @RestClient private val restClient: PaymentMethodRestClient
) : PaymentMethodService {

    override fun getAll(): Set<PaymentMethod> {
        return restClient.getAll()
    }

    override fun getAllAsync(): CompletionStage<PaymentMethod> {
        return restClient.getAllAsync()
    }

    override fun getAllAsUni(): Uni<PaymentMethod> {
        return restClient.getAllAsUni()
    }

    override fun getById(id: String): Set<PaymentMethod> {
        return restClient.getById(id)
    }

    override fun getByIdAsync(id: String): CompletionStage<Set<PaymentMethod>> {
        return restClient.getByIdAsync(id)
    }

    override fun getByIdAsUni(id: String): Uni<Set<PaymentMethod>> {
        return restClient.getByIdAsUni(id)
    }
}
