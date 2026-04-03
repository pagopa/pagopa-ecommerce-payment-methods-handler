package it.pagopa.ecommerce.payment.methods.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.redis.datasource.ReactiveRedisDataSource
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
    private val redisDataSource: ReactiveRedisDataSource,
    private val objectMapper: ObjectMapper,
    @ConfigProperty(name = "npg.sessions.ttl-seconds", defaultValue = "600")
    private val ttlSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(NpgSessionsRedisWrapper::class.java)
    private val keyPrefix = "npg:"

    fun save(document: NpgSessionDocument): Uni<NpgSessionDocument> {
        val key = keyPrefix + document.orderId
        val json = objectMapper.writeValueAsString(document)
        val commands = redisDataSource.value(String::class.java, String::class.java)
        return commands.setex(key, ttlSeconds, json).replaceWith(document).onFailure().invoke { e ->
            log.error("Error saving NPG session for orderId=${document.orderId}", e)
        }
    }

    fun findById(orderId: String): Uni<NpgSessionDocument?> {
        val key = keyPrefix + orderId
        val commands = redisDataSource.value(String::class.java, String::class.java)
        return commands.get(key).onItem().ifNotNull().transform { json ->
            objectMapper.readValue(json, NpgSessionDocument::class.java)
        }
    }
}
