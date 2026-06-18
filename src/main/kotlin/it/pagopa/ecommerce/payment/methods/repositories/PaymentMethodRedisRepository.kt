package it.pagopa.ecommerce.payment.methods.repositories

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class PaymentMethodRedisRepository
@Inject
constructor(
    private val redisDataSource: ReactiveRedisDataSource,
    @ConfigProperty(name = "payment-methods.cache.ttl-seconds")
    private val ttlSeconds: Long,
) {
    private val keyPrefix = "handler-payment-methods-cache:"
    private val commands = redisDataSource.value(PaymentMethodResponse::class.java)

    fun findById(paymentMethodId: String): Uni<PaymentMethodResponse?> {
        return commands.get("$keyPrefix$paymentMethodId")
    }

    fun save(paymentMethod: PaymentMethodResponse): Uni<Unit> {
        return commands
            .setex(
                "$keyPrefix${paymentMethod.id}",
                ttlSeconds,
                paymentMethod
            )
            .map { Unit }
    }
}
