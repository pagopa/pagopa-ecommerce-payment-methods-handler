package utils

import io.quarkus.vertx.web.RouteFilter
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@ApplicationScoped
class ApiKeyFilter {

    companion object {
        private val LOG: Logger = Logger.getLogger(ApiKeyFilter::class.java)
    }

    @ConfigProperty(name = "security.apiKey.primary") lateinit var primaryApiKey: String

    @ConfigProperty(name = "security.apiKey.secondary") lateinit var secondaryApiKey: String

    @ConfigProperty(name = "security.apiKey.securedPaths") lateinit var securedPaths: List<String>

    private val validApiKeys: Set<String>
        get() = setOf(primaryApiKey, secondaryApiKey)

    @RouteFilter
    fun filter(ctx: RoutingContext) {
        val path = ctx.request().path()

        if (securedPaths.any { path.startsWith(it) }) {
            val apiKey = ctx.request().getHeader("x-api-key")

            if (!isValidApiKey(apiKey)) {
                LOG.errorf("Unauthorized request for path %s - Missing or invalid API key", path)
                ctx.response().setStatusCode(401).end()
                return
            }

            logWhichApiKey(apiKey, path)
        }

        ctx.next()
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
        LOG.debugf("API key type used for path %s: %s", path, apiKeyType)
    }
}
