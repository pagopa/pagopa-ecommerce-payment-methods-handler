package it.pagopa.ecommerce.payment.methods.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.redis.datasource.value.ReactiveValueCommands
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.domain.NpgSessionDocument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NpgSessionsRedisWrapperTest {

    private val valueCommands = mock<ReactiveValueCommands<String, NpgSessionDocument>>()
    private val redisDataSource =
        mock<ReactiveRedisDataSource> {
            on { value(String::class.java, NpgSessionDocument::class.java) } doReturn valueCommands
        }
    private val ttlSeconds = 600L

    private val objectMapper = mock<ObjectMapper> { on { writeValueAsString(any()) } doReturn "{}" }
    private val wrapper = NpgSessionsRedisWrapper(redisDataSource, objectMapper, ttlSeconds)

    private val testDocument =
        NpgSessionDocument(
            orderId = "order-123",
            correlationId = "corr-456",
            sessionId = "session-789",
            securityToken = "sec-token",
        )

    @Test
    fun `should save session document to Redis`() {
        val expectedKey = "npg:order-123"

        doReturn(Uni.createFrom().voidItem())
            .whenever(valueCommands)
            .setex(eq(expectedKey), eq(ttlSeconds), eq(testDocument))

        val result = wrapper.save(testDocument).await().indefinitely()

        assertEquals(testDocument, result)
        verify(valueCommands).setex(expectedKey, ttlSeconds, testDocument)
    }

    @Test
    fun `should find session document by orderId`() {
        val expectedKey = "npg:order-123"

        doReturn(Uni.createFrom().item(testDocument)).whenever(valueCommands).get(eq(expectedKey))

        val result = wrapper.findById("order-123").await().indefinitely()

        assertNotNull(result)
        assertEquals("order-123", result!!.orderId)
        assertEquals("corr-456", result.correlationId)
        assertEquals("session-789", result.sessionId)
        assertEquals("sec-token", result.securityToken)
    }

    @Test
    fun `should return null when session not found`() {
        val expectedKey = "npg:unknown-order"

        doReturn(Uni.createFrom().nullItem<NpgSessionDocument>())
            .whenever(valueCommands)
            .get(eq(expectedKey))

        val result = wrapper.findById("unknown-order").await().indefinitely()

        assertNull(result)
    }

    @Test
    fun `should propagate error when Redis save fails`() {
        val expectedKey = "npg:order-123"
        val redisError = RuntimeException("Redis write error")

        doReturn(Uni.createFrom().failure<Void>(redisError))
            .whenever(valueCommands)
            .setex(eq(expectedKey), eq(ttlSeconds), any())

        val thrown =
            assertThrows<RuntimeException> { wrapper.save(testDocument).await().indefinitely() }

        assertEquals("Redis write error", thrown.message)
    }

    @Test
    fun `should save session with card data`() {
        val docWithCardData =
            testDocument.copy(
                cardData =
                    it.pagopa.ecommerce.payment.methods.domain.CardDataDocument(
                        bin = "123456",
                        lastFourDigits = "7890",
                        expiringDate = "122025",
                        circuit = "VISA",
                    )
            )
        val expectedKey = "npg:order-123"

        doReturn(Uni.createFrom().voidItem())
            .whenever(valueCommands)
            .setex(eq(expectedKey), eq(ttlSeconds), eq(docWithCardData))

        val result = wrapper.save(docWithCardData).await().indefinitely()

        assertEquals(docWithCardData, result)
    }

    @Test
    fun `should execute Lua script and return OK on associateTransaction`() {
        val requestedTxId = "tx-001"
        val mockResponse =
            mock<io.vertx.mutiny.redis.client.Response> { on { toString() } doReturn "OK" }

        doReturn(Uni.createFrom().item(mockResponse))
            .whenever(redisDataSource)
            .execute(eq("EVAL"), any(), eq("1"), any(), eq(requestedTxId), any())

        val result =
            wrapper.associateTransaction(testDocument, requestedTxId).await().indefinitely()

        assertEquals(NpgSessionsRedisWrapper.AssociateOutcome.OK, result.outcome)
        assertNull(result.existingTransactionId)
    }

    @Test
    fun `should execute Lua script and return NOT_FOUND on associateTransaction`() {
        val requestedTxId = "tx-001"
        val mockResponse =
            mock<io.vertx.mutiny.redis.client.Response> { on { toString() } doReturn "NOT_FOUND" }

        doReturn(Uni.createFrom().item(mockResponse))
            .whenever(redisDataSource)
            .execute(eq("EVAL"), any(), eq("1"), any(), eq(requestedTxId), any())

        val result =
            wrapper.associateTransaction(testDocument, requestedTxId).await().indefinitely()

        assertEquals(NpgSessionsRedisWrapper.AssociateOutcome.NOT_FOUND, result.outcome)
        assertNull(result.existingTransactionId)
    }

    @Test
    fun `should execute Lua script and return CONFLICT with existing id on associateTransaction`() {
        val requestedTxId = "tx-new"
        val existingTxId = "tx-old"
        val mockResponse =
            mock<io.vertx.mutiny.redis.client.Response> {
                on { toString() } doReturn "CONFLICT:$existingTxId"
            }

        doReturn(Uni.createFrom().item(mockResponse))
            .whenever(redisDataSource)
            .execute(eq("EVAL"), any(), eq("1"), any(), eq(requestedTxId), any())

        val result =
            wrapper.associateTransaction(testDocument, requestedTxId).await().indefinitely()

        assertEquals(NpgSessionsRedisWrapper.AssociateOutcome.CONFLICT, result.outcome)
        assertEquals(existingTxId, result.existingTransactionId)
    }

    @Test
    fun `should propagate error when Lua script execution fails`() {
        val requestedTxId = "tx-001"
        val redisError = RuntimeException("Script error")

        doReturn(Uni.createFrom().failure<io.vertx.mutiny.redis.client.Response>(redisError))
            .whenever(redisDataSource)
            .execute(eq("EVAL"), any(), eq("1"), any(), eq(requestedTxId), any())

        val thrown =
            assertThrows<RuntimeException> {
                wrapper.associateTransaction(testDocument, requestedTxId).await().indefinitely()
            }

        assertEquals("Script error", thrown.message)
    }
}
