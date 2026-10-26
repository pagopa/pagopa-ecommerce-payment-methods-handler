package it.pagopa.ecommerce.payment.methods.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import it.pagopa.ecommerce.payment.methods.exception.LogTracingUtilException
import java.util.EnumMap
import org.slf4j.Logger
import org.slf4j.MDC
import org.slf4j.event.Level

/**
 * Utility class for structured logging utilizing the Fluent Builder pattern.
 *
 * This class facilitates the population of the SLF4J Mapped Diagnostic Context (MDC) with
 * predefined attributes, custom details, and error information. It ensures that MDC keys are safely
 * added before logging and properly cleaned up immediately after the log is emitted, preventing
 * context leaks in concurrent environments.
 */
class LogTracingUtils private constructor() {

    private var message: String = ""
    private var outcome: String = ""
    private var stackTrace: String? = null
    private var error: Throwable? = null
    private val attributes: EnumMap<AttributeKeys, String> = EnumMap(AttributeKeys::class.java)
    private var details: MutableMap<String, String> = mutableMapOf()
    val mdcKeys: MutableList<String> = mutableListOf()

    private var logger: Logger? = null

    companion object {

        private val objectMapper =
            ObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        /**
         * Creates a new instance of the log builder.
         *
         * @return a new {@link LogTracingUtils} instance
         */
        fun loggerTracingUtils(): LogTracingUtils = LogTracingUtils()

        /**
         * Serializes the custom details map into a JSON string.
         *
         * @param details the map to serialize
         * @return a JSON formatted string, or "{}" if serialization fails or map is null
         */
        private fun serializeDetailsToMdcMap(details: Map<String, *>): String {
            val rawDetails = "{}"
            if (details.isNotEmpty()) {
                try {
                    return objectMapper.writeValueAsString(details)
                } catch (e: Exception) {
                    // Log the error and return an empty JSON object
                    return rawDetails
                }
            }
            return rawDetails
        }

        const val SUCCESS: String = "success"

        const val FAILURE: String = "failure"

        /** Dependency value used in MDC for MongoDB operations. */
        const val MONGO_DEPENDENCY: String = "eCommerce-mongodb"

        /** Dependency value used in MDC for Redis operations. */
        const val REDIS_DEPENDENCY: String = "eCommerce-redis"

        /** Dependency value used in MDC for storage queue operations. */
        const val STORAGE_QUEUE_DEPENDENCY: String = "storage-queue"

        /** Dependency value used in MDC for NPG operations. */
        const val NPG_DEPENDENCY: String = "npg"

        /** Dependency value used in MDC for NODO operations. */
        const val NODO_DEPENDENCY: String = "nodo"

        /** Dependency value used in MDC for payment wallet operations. */
        const val WALLET_DEPENDENCY: String = "payment-wallet"

        /** Dependency value used in MDC for JWT issuer operations. */
        const val JWT_ISSUER_DEPENDENCY: String = "jwt-issuer"

        /** Dependency value used in MDC for payment methods service operations. */
        const val PAYMENT_METHODS_SERVICE_DEPENDENCY: String = "payment-methods-service"

        /** Dependency value used in MDC for payment methods handler operations. */
        const val PAYMENT_METHODS_HANDLER_DEPENDENCY: String = "payment-methods-handler"

        /** Dependency value used in MDC for Redirect operations. */
        const val REDIRECT_DEPENDENCY: String = "redirect"

        /** Dependency value used in MDC for Notifications service operations. */
        const val NOTIFICATIONS_SERVICE_DEPENDENCY: String = "notifications-service"

        /** Enumeration of standard public keys used for MDC and Reactor context population. */
        enum class AttributeKeys(val key: String, defaultValue: String) {
            /** Reactor context key for action associated with the event. */
            EVENT_ACTION("event_action", "{eventAction-not-found}"),

            /** Reactor context key for transaction identifier. */
            CTX_TRANSACTION_ID("ctx_transaction_id", "{transactionId-not-found}"),

            /** Reactor context key for authorization request identifier. */
            CTX_AUTHORIZATION_REQUEST_ID(
                "ctx_authorization_request_id",
                "{authorizationRequestId-not-found}",
            ),

            /** Reactor context key for event code. */
            CTX_EVENT_CODE("ctx_event_code", "{eventCode-not-found}"),

            /** Reactor context key for event identifier. */
            CTX_EVENT_ID("ctx_event_id", "{eventId-not-found}"),

            /** Reactor context key for RPT identifiers. */
            CTX_RPT_IDS("ctx_rpt_ids", "{rptIds-not-found}"),

            /** Reactor context key for payment tokens. */
            CTX_PAYMENT_TOKENS("ctx_payment_tokens", "{paymentTokens-not-found}"),

            /** Reactor context key for user identifier. */
            CTX_USER_ID("ctx_user_id", "{userId-not-found}"),

            /** MDC key for correlation identifier. */
            CORRELATION_ID("correlation_id", "{correlationId-not-found}"),

            /** MDC key for PSP identifier. */
            PSP_ID("psp_id", "{pspId-not-found}"),

            /** Reactor context key for wallet id. */
            CTX_WALLET_ID("ctx_wallet_id", "{walletId-not-found}"),

            /** Reactor context key for client id. */
            CTX_CLIENT_ID("ctx_client_id", "{clientId-not-found}");

            private val defaultValue: String?

            init {
                this.defaultValue = defaultValue
            }
        }

        /** Enumeration of internal/private MDC keys handled exclusively by the builder. */
        private enum class AttributeKeysPrivate(val key: String, defaultValue: String) {
            /** MDC key for custom JSON details map. */
            CTX_DETAILS("ctx_details", "{details-not-found}"),

            /** MDC key for event outcome. */
            EVENT_OUTCOME("event_outcome", "{eventOutcome-not-found}"),

            /**
             * Details key for dependency name involved in the operation (serialized inside
             * ctx.details).
             */
            DEPENDENCY("dependency", "{dependency-not-found}"),

            /** MDC key for error class name. */
            ERROR_TYPE("error.type", "{errorType-not-found}"),

            /** MDC key for error message text. */
            ERROR_MESSAGE("error.message", "{errorMessage-not-found}"),

            /** MDC key for the complete error stack trace. */
            ERROR_STACK_TRACE("error.stack_trace", "{errorStackTrace-not-found}");

            private val defaultValue: String?

            init {
                this.defaultValue = defaultValue
            }
        }
    }

    /**
     * Sets a map of standard attributes to be added to the MDC.
     *
     * @param attributes map of standard {@link AttributeKeys} and their values
     * @return this builder instance
     */
    fun attributes(attributes: Map<AttributeKeys, String>): LogTracingUtils {
        this.attributes.putAll(attributes)
        return this
    }

    /**
     * Sets the external dependency name involved in the current operation.
     *
     * @param dependency the name of the dependency
     * @return this builder instance
     */
    fun dependency(dependency: String): LogTracingUtils {
        this.details[AttributeKeysPrivate.DEPENDENCY.key] = dependency
        return this
    }

    /**
     * Marks the outcome of the logged event as successful.
     *
     * @return this builder instance
     */
    fun success(): LogTracingUtils {
        this.outcome = SUCCESS
        return this
    }

    /**
     * Marks the outcome of the logged event as a failure.
     *
     * @return this builder instance
     */
    fun failure(): LogTracingUtils {
        this.outcome = FAILURE
        return this
    }

    /**
     * Adds a key-value pair to the MDC and tracks it for post-log cleanup.
     *
     * @param key the MDC key
     * @param value the MDC value
     */
    private fun addMdcKey(key: String, value: String?) {
        MDC.put(key, value)
        this.mdcKeys.add(key)
    }

    /**
     * Core logging routine. Populates the MDC with attributes, details, and errors, issues the log
     * statement using the requested level, and immediately performs MDC cleanup to prevent context
     * leaks.
     *
     * @param loggerLevel the SLF4J log level
     */
    private fun log(loggerLevel: Level) {
        // Add attributes keys and valus to MDC map
        if (attributes.isNotEmpty()) {
            attributes.forEach { (key, value) -> run { addMdcKey(key.key, value) } }
        }

        // Add details key and value to MDC map
        if (this.details.isNotEmpty()) {
            addMdcKey(AttributeKeysPrivate.CTX_DETAILS.key, serializeDetailsToMdcMap(this.details))
        }

        addMdcKey(AttributeKeysPrivate.EVENT_OUTCOME.key, this.outcome)

        this.error?.let { error ->
            addMdcKey(AttributeKeysPrivate.ERROR_TYPE.key, error.javaClass.name)
        }

        this.stackTrace?.let { stackTrace ->
            addMdcKey(AttributeKeysPrivate.ERROR_STACK_TRACE.key, stackTrace)
        }

        try {
            when (loggerLevel) {
                Level.INFO -> logger?.info(this.message)
                Level.ERROR -> logger?.error(this.message, this.error)
                Level.WARN -> logger?.warn(this.message)
                Level.DEBUG -> logger?.debug(this.message)
                Level.TRACE -> logger?.trace(this.message)
                else -> throw LogTracingUtilException("loggerLevel null or not defined.")
            }
        } finally {
            // Clean up MDC keys after logging
            this.mdcKeys.forEach { key -> MDC.remove(key) }
            this.mdcKeys.clear()
        }
    }

    fun logInfo(logger: Logger, message: String) {
        this.message = message
        this.logger = logger
        log(Level.INFO)
    }

    fun logDebug(logger: Logger, message: String) {
        this.message = message
        this.logger = logger
        log(Level.DEBUG)
    }

    fun logWarn(logger: Logger, message: String) {
        this.message = message
        this.logger = logger
        log(Level.WARN)
    }

    fun logError(logger: Logger, throwable: Throwable?, message: String) {
        this.message = message
        this.logger = logger
        this.error = throwable
        log(Level.ERROR)
    }

    fun logError(logger: Logger, message: String) {
        this.message = message
        this.logger = logger
        logError(logger, null, message)
    }

    fun logWithStackTrace(logger: Logger, throwable: Throwable, message: String) {
        this.message = message
        this.logger = logger
        this.error = throwable
        this.stackTrace = throwable.stackTraceToString()
        log(Level.ERROR)
    }
}
