package it.pagopa.ecommerce.payment.methods.utils

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.resteasy.reactive.server.ServerRequestFilter
import org.slf4j.LoggerFactory

class ApiKeyFilter {

    companion object {
        private val LOG = LoggerFactory.getLogger(ApiKeyFilter::class.java)
    }

    @ConfigProperty(name = "security.apiKey.primary") lateinit var primaryApiKey: String

    @ConfigProperty(name = "security.apiKey.secondary") lateinit var secondaryApiKey: String

    @ConfigProperty(name = "security.apiKey.securedPaths") lateinit var securedPaths: List<String>

    private val validApiKeys: Set<String>
        get() = setOf(primaryApiKey, secondaryApiKey)

    @ServerRequestFilter
    fun filter(ctx: ContainerRequestContext): Response? {
        val path = ctx.uriInfo.path

        if (securedPaths.any { path.startsWith(it) }) {
            val apiKey = ctx.getHeaderString("x-api-key")

            if (!isValidApiKey(apiKey)) {
                LogTracingUtils.loggerTracingUtils()
                    .failure()
                    .logWarn(LOG, "Unauthorized request - Missing or invalid API key")

                return Response.status(Response.Status.UNAUTHORIZED).build()
            }

            logWhichApiKey(apiKey, path)
        }
        return null
    }

    private fun isValidApiKey(apiKey: String?): Boolean {
        return !apiKey.isNullOrBlank() && validApiKeys.contains(apiKey)
    }

    private fun logWhichApiKey(apiKey: String?, path: String) {
        val apiKeyType =
            when (apiKey) {
                primaryApiKey -> "primary"
                secondaryApiKey -> "secondary"
                else -> "unknown"
            }
        if (LOG.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .details(mapOf("api_key_type" to apiKeyType, "path" to path))
                .logDebug(LOG, "API key type used details")
        }
    }
}
