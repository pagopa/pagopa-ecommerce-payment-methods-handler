package it.pagopa.ecommerce.payment.methods.utils

import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import org.jboss.resteasy.reactive.server.ServerRequestFilter

/*
 * This class is responsible for enriching the logs with additional information about the API request.
 */
class ApiLogsEnricherFilter {

    @ServerRequestFilter(priority = Priorities.AUTHENTICATION - 1)
    fun enrichLogs(ctx: ContainerRequestContext) {
        LogTracingUtils.enrichContext(
            LogTracingUtils.Companion.AttributeKeys.EVENT_ACTION,
            ctx.method + " " + ctx.uriInfo.path,
        )
    }
}
