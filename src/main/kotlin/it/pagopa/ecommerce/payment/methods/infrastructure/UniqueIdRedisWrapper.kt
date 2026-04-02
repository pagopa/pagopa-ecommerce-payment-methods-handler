package it.pagopa.ecommerce.payment.methods.infrastructure

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Redis wrapper for unique ID generation. Encapsulates SETNX + EXPIRE operations, making the
 * UniqueIdGenerator easily testable.
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
     * Attempts to save the unique ID using SETNX (set if not exists). If the key does not exist,
     * sets it with a TTL and returns true. If the key already exists, returns false (collision).
     */
    fun saveIfAbsent(uniqueId: String): Uni<Boolean> {
        val redisKey = "$KEYSPACE:$uniqueId"
        val commands = redisDataSource.value(String::class.java, String::class.java)
        return commands.setnx(redisKey, uniqueId).flatMap { wasSet ->
            if (wasSet) {
                redisDataSource.key().expire(redisKey, TTL_SECONDS).replaceWith(true)
            } else {
                Uni.createFrom().item(false)
            }
        }
    }
}
