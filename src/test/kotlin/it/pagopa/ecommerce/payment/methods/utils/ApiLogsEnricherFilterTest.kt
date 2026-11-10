package it.pagopa.ecommerce.payment.methods.utils

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import it.pagopa.ecommerce.payment.methods.TestUtils
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.UriInfo
import java.util.concurrent.CopyOnWriteArrayList
import org.jboss.logmanager.ExtHandler
import org.jboss.logmanager.ExtLogRecord
import org.jboss.logmanager.Logger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.MDC

@QuarkusTest
class ApiLogsEnricherFilterTest {

    private val filter = ApiLogsEnricherFilter()

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @ParameterizedTest
    @CsvSource(
        "POST, /payment-methods, POST /payment-methods",
        "GET, /payment-methods/card-visa, GET /payment-methods/card-visa",
        "POST, /payment-methods/card-visa/fees, POST /payment-methods/card-visa/fees",
        "PATCH, /payment-methods/card-visa/sessions/order-123, PATCH /payment-methods/card-visa/sessions/order-123",
        "GET, /npg/sdk/integrity, GET /npg/sdk/integrity",
    )
    fun `enrichment includes the HTTP method and actual request path`(
        method: String,
        path: String,
        expectedAction: String,
    ) {
        filter.enrichLogs(requestContext(method, path))

        assertEquals(expectedAction, MDC.get("event_action"))
    }

    @Test
    fun `enrichment replaces the previous action without changing other request context`() {
        MDC.put("event_action", "POST /payment-methods")
        MDC.put("ctx_request_id", "request-123")
        MDC.put("ctx_client_id", "CHECKOUT")

        filter.enrichLogs(requestContext("GET", "/payment-methods/card-visa"))

        assertEquals("GET /payment-methods/card-visa", MDC.get("event_action"))
        assertEquals("request-123", MDC.get("ctx_request_id"))
        assertEquals("CHECKOUT", MDC.get("ctx_client_id"))
    }

    @Test
    fun `API key rejection log already contains the action without query parameters`() {
        val actions = CopyOnWriteArrayList<String?>()
        val handler =
            object : ExtHandler() {
                override fun doPublish(record: ExtLogRecord) {
                    actions.add(record.getMdc("event_action"))
                }
            }
        val logger = Logger.getLogger(ApiKeyFilter::class.java.name)
        logger.addHandler(handler)

        try {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("x-api-key", "invalid-key")
                .header("x-client-id", "CHECKOUT")
                .header("x-language", "IT")
                .queryParam("maxOccurrences", 10)
                .body(TestUtils.buildCalculateFeeRequest())
                .`when`()
                .post("/payment-methods/card-visa/fees")
                .then()
                .statusCode(401)

            assertEquals(listOf("POST /payment-methods/card-visa/fees"), actions.toList())
        } finally {
            logger.removeHandler(handler)
            handler.close()
        }
    }

    private fun requestContext(method: String, path: String): ContainerRequestContext {
        val uriInfo = mock<UriInfo>()
        whenever(uriInfo.path).thenReturn(path)
        val context = mock<ContainerRequestContext>()
        whenever(context.method).thenReturn(method)
        whenever(context.uriInfo).thenReturn(uriInfo)
        return context
    }
}
