package ecommerce.services

import ecommerce.dto.PaymentMethod
import io.smallrye.mutiny.Uni
import java.util.concurrent.CompletionStage

interface PaymentMethodService {

    fun getAll(): Set<PaymentMethod>

    fun getAllAsync(): CompletionStage<PaymentMethod>

    fun getAllAsUni(): Uni<PaymentMethod>

    fun getById(id: String): Set<PaymentMethod>

    fun getByIdAsync(id: String): CompletionStage<Set<PaymentMethod>>

    fun getByIdAsUni(id: String): Uni<Set<PaymentMethod>>
}
