package it.pagopa.ecommerce.payment.methods.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.domain.UniqueIdDocument
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Redis wrapper for unique ID generation. Uses atomic SET NX EX to guarantee uniqueness, matching
 * the behavior of ecommerce-commons ReactiveUniqueIdTemplateWrapper.saveIfAbsent which maps to
 * Spring's ValueOperations.setIfAbsent(key, value, Duration).
 *
 */
@ApplicationScoped
class UniqueIdRedisWrapper
@Inject
constructor(
    private val redisDataSource: ReactiveRedisDataSource,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val KEYSPACE = "uniqueId"
        private const val TTL_SECONDS = 60L
    }

    fun saveIfAbsent(uniqueId: String): Uni<Boolean> {
        val redisKey = "$KEYSPACE:$uniqueId"
        val value = objectMapper.writeValueAsString(UniqueIdDocument(uniqueId))

        return redisDataSource
            .execute("SET", redisKey, value, "NX", "EX", TTL_SECONDS.toString())
            .map { response -> response != null }
    }
}
