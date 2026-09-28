package it.pagopa.ecommerce.payment.methods.infrastructure

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.redis.datasource.value.ReactiveValueCommands
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.domain.NpgSessionDocument
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory

@ApplicationScoped
class NpgSessionsRedisWrapper
@Inject
constructor(
    redisDataSource: ReactiveRedisDataSource,
    @ConfigProperty(name = "npg.sessions.ttl-seconds", defaultValue = "600")
    private val ttlSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(NpgSessionsRedisWrapper::class.java)
    private val keyPrefix = "npg:"

    private val commands: ReactiveValueCommands<String, NpgSessionDocument> =
        redisDataSource.value(String::class.java, NpgSessionDocument::class.java)

    fun save(document: NpgSessionDocument): Uni<NpgSessionDocument> {
        val key = keyPrefix + document.orderId
        return commands.setex(key, ttlSeconds, document).replaceWith(document).onFailure().invoke {
            e ->
            log.error("Error saving NPG session for orderId=${document.orderId}", e)
        }
    }

    fun findById(orderId: String): Uni<NpgSessionDocument?> {
        return commands.get(keyPrefix + orderId)
    }
}
