package it.pagopa.ecommerce.payment.methods.utils

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.UniqueIdGenerationException
import it.pagopa.ecommerce.payment.methods.infrastructure.UniqueIdRedisWrapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.security.SecureRandom
import org.slf4j.LoggerFactory

/**
 * Generates unique IDs using Redis SETNX to guarantee uniqueness. Replicates the behavior of
 * ecommerce-commons ReactiveUniqueIdUtils:
 * - ID format: "E" + timestamp millis + random alphanumeric suffix (total 18 chars)
 * - Uses Redis SET NX (set if not exists) with 60s TTL to detect collisions
 * - Retries up to 3 times on collision
 */
@ApplicationScoped
class UniqueIdGenerator
@Inject
constructor(private val uniqueIdRedisWrapper: UniqueIdRedisWrapper) {

    private val log = LoggerFactory.getLogger(UniqueIdGenerator::class.java)

    companion object {
        private val secureRandom = SecureRandom()
        private const val ALPHANUMERICS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
        const val MAX_LENGTH = 18
        const val MAX_ATTEMPTS = 3
        const val PRODUCT_PREFIX = "E"
    }

    fun generateUniqueId(): Uni<String> {
        return tryGenerate(1)
    }

    private fun tryGenerate(attempt: Int): Uni<String> {
        if (attempt > MAX_ATTEMPTS) {
            return Uni.createFrom().failure(UniqueIdGenerationException())
        }

        val uniqueId = generateRandomIdentifier()

        return uniqueIdRedisWrapper
            .saveIfAbsent(uniqueId)
            .flatMap { wasSet ->
                if (wasSet) {
                    Uni.createFrom().item(uniqueId)
                } else {
                    log.warn("UniqueId collision on attempt $attempt for id=$uniqueId, retrying...")
                    tryGenerate(attempt + 1)
                }
            }
            .onFailure()
            .invoke { e ->
                if (e !is UniqueIdGenerationException) {
                    log.error("Error generating unique id on attempt $attempt", e)
                }
            }
    }

    private fun generateRandomIdentifier(): String {
        val sb = StringBuilder(PRODUCT_PREFIX)
        sb.append(System.currentTimeMillis())
        val randomLength = MAX_LENGTH - sb.length
        repeat(randomLength) {
            sb.append(ALPHANUMERICS[secureRandom.nextInt(ALPHANUMERICS.length)])
        }
        return sb.toString()
    }
}
