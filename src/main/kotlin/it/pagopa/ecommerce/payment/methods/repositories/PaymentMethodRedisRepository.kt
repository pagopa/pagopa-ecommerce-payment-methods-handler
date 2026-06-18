package it.pagopa.ecommerce.payment.methods.repositories

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
    @ConfigProperty(name = "payment-methods.cache.ttl-seconds", defaultValue = "3600")
    private val ttlSeconds: Long,
) {
    private val keyPrefix = "payment-methods:"
    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    private val commands = redisDataSource.value(String::class.java)

    fun findById(paymentMethodId: String): Uni<PaymentMethodResponse?> {
        return commands.get("$keyPrefix$paymentMethodId").map { json ->
            json?.let { objectMapper.readValue(it, PaymentMethodResponse::class.java) }
        }
    }

    fun save(paymentMethod: PaymentMethodResponse): Uni<Void> {
        val json = objectMapper.writeValueAsString(paymentMethod)
        return commands.setex("$keyPrefix${paymentMethod.id}", ttlSeconds, json)
    }
}
