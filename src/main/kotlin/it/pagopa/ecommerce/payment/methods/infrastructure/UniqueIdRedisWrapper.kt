package it.pagopa.ecommerce.payment.methods.infrastructure

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Redis wrapper for unique ID generation. Uses atomic SET NX EX to guarantee uniqueness, matching
 * the behavior of ecommerce-commons RedisTemplateWrapper.saveIfAbsent which maps to Spring's
 * ValueOperations.setIfAbsent(key, value, Duration).
 */
@ApplicationScoped
class UniqueIdRedisWrapper
@Inject
constructor(private val redisDataSource: ReactiveRedisDataSource) {

    companion object {
        private const val KEYSPACE = "uniqueId"
        private const val TTL_SECONDS = 60L
    }

    fun saveIfAbsent(uniqueId: String): Uni<Boolean> {
        val redisKey = "$KEYSPACE:$uniqueId"

        return redisDataSource
            .execute("SET", redisKey, uniqueId, "NX", "EX", TTL_SECONDS.toString())
            .map { response -> response != null }
    }
}
