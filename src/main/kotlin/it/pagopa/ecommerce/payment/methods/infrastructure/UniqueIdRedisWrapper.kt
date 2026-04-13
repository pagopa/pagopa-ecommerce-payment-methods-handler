package it.pagopa.ecommerce.payment.methods.infrastructure

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.redis.datasource.value.SetArgs
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

    /**
     * Atomically sets the unique ID using SET NX EX (set if not exists with expiry). Returns true
     * if the key was set, false if it already existed.
     *
     * Uses setGet with NX+EX args: returns null when key was set (no previous value), returns the
     * existing value when key already exists.
     */
    fun saveIfAbsent(uniqueId: String): Uni<Boolean> {
        val redisKey = "$KEYSPACE:$uniqueId"
        val commands = redisDataSource.value(String::class.java, String::class.java)
        return commands.setGet(redisKey, uniqueId, SetArgs().nx().ex(TTL_SECONDS)).map {
            previousValue ->
            previousValue == null
        }
    }
}
