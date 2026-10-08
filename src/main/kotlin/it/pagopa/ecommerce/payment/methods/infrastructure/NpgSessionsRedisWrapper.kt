package it.pagopa.ecommerce.payment.methods.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
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
    private val redisDataSource: ReactiveRedisDataSource,
    private val objectMapper: ObjectMapper,
    @ConfigProperty(name = "npg.sessions.ttl-seconds", defaultValue = "600")
    private val ttlSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(NpgSessionsRedisWrapper::class.java)
    private val keyPrefix = "npg:"

    private val commands: ReactiveValueCommands<String, NpgSessionDocument> =
        redisDataSource.value(String::class.java, NpgSessionDocument::class.java)

    /** Possible outcomes of an atomic transaction-to-session association. */
    enum class AssociateOutcome {
        /** Transaction successfully associated, or already associated to the same transaction. */
        OK,
        /** No session found for the given orderId. */
        NOT_FOUND,
        /** Session already associated to a different transaction. */
        CONFLICT,
    }

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

    /**
     * Atomically associates a transaction id to an existing NPG session, enforcing the
     * single-transaction-per-session guarantee server-side.
     *
     * The whole read-decide-write happens inside a single Redis Lua script, so two concurrent
     * requests cannot both observe a null transaction id and overwrite each other. The script:
     * - returns NOT_FOUND if the session key does not exist;
     * - writes the updated document (preserving the remaining TTL) and returns OK when the session
     *   has no transaction id yet;
     * - returns OK (no-op) when the session is already associated to the same transaction id
     *   (idempotent retry);
     * - returns CONFLICT when the session is already associated to a different transaction id.
     *
     * @param updatedDocument the session document already populated with the requested transaction
     *   id; its JSON is written verbatim when the association succeeds.
     * @param requestedTransactionId the transaction id to associate.
     */
    fun associateTransaction(
        updatedDocument: NpgSessionDocument,
        requestedTransactionId: String,
    ): Uni<AssociateResult> {
        val key = keyPrefix + updatedDocument.orderId
        val newDocumentJson = objectMapper.writeValueAsString(updatedDocument)

        // KEYS[1] = redis key, ARGV[1] = requestedTransactionId, ARGV[2] = updated document JSON
        val script =
            """
            local current = redis.call('GET', KEYS[1])
            if current == false then
                return 'NOT_FOUND'
            end
            local existing = string.match(current, '"transactionId"%s*:%s*"([^"]*)"')
            if existing == nil then
                local pttl = redis.call('PTTL', KEYS[1])
                if pttl and pttl > 0 then
                    redis.call('SET', KEYS[1], ARGV[2], 'PX', pttl)
                else
                    redis.call('SET', KEYS[1], ARGV[2])
                end
                return 'OK'
            end
            if existing == ARGV[1] then
                return 'OK'
            end
            return 'CONFLICT:' .. existing
            """
                .trimIndent()

        return redisDataSource
            .execute("EVAL", script, "1", key, requestedTransactionId, newDocumentJson)
            .map { response -> parseOutcome(response.toString()) }
            .onFailure()
            .invoke { e ->
                log.error(
                    "Error associating transaction to NPG session for orderId=${updatedDocument.orderId}",
                    e,
                )
            }
    }

    private fun parseOutcome(raw: String): AssociateResult {
        return when {
            raw == "OK" -> AssociateResult(AssociateOutcome.OK, null)
            raw == "NOT_FOUND" -> AssociateResult(AssociateOutcome.NOT_FOUND, null)
            raw.startsWith("CONFLICT:") ->
                AssociateResult(AssociateOutcome.CONFLICT, raw.removePrefix("CONFLICT:"))
            else -> AssociateResult(AssociateOutcome.CONFLICT, null)
        }
    }

    /** Result of an atomic association, carrying the existing transaction id on conflict. */
    data class AssociateResult(val outcome: AssociateOutcome, val existingTransactionId: String?)
}
