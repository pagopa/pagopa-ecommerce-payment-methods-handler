package it.pagopa.ecommerce.payment.methods.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.redis.client.Response
import it.pagopa.ecommerce.payment.methods.domain.UniqueIdDocument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UniqueIdRedisWrapperTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val redisDataSource = mock<ReactiveRedisDataSource>()
    private val wrapper = UniqueIdRedisWrapper(redisDataSource, objectMapper)

    @Test
    fun `should return true when key does not exist and is set successfully`() {
        val mockResponse = mock<Response>()

        doReturn(Uni.createFrom().item(mockResponse))
            .whenever(redisDataSource)
            .execute(
                eq("SET"),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
            )

        val result = wrapper.saveIfAbsent("E1234567890123abcd").await().indefinitely()

        assertTrue(result)
    }

    @Test
    fun `should return false when key already exists`() {
        doReturn(Uni.createFrom().nullItem<Response>())
            .whenever(redisDataSource)
            .execute(
                eq("SET"),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
            )

        val result = wrapper.saveIfAbsent("E1234567890123abcd").await().indefinitely()

        assertFalse(result)
    }

    @Test
    fun `should store a UniqueIdDocument as JSON with the correct key and NX EX arguments`() {
        val mockResponse = mock<Response>()
        val argsCaptor = argumentCaptor<String>()

        doReturn(Uni.createFrom().item(mockResponse))
            .whenever(redisDataSource)
            .execute(
                eq("SET"),
                argsCaptor.capture(),
                argsCaptor.capture(),
                argsCaptor.capture(),
                argsCaptor.capture(),
                argsCaptor.capture(),
            )

        wrapper.saveIfAbsent("E1234567890123abcd").await().indefinitely()

        val captured = argsCaptor.allValues
        assertEquals("uniqueId:E1234567890123abcd", captured[0])
        assertEquals("NX", captured[2])
        assertEquals("EX", captured[3])
        assertEquals("60", captured[4])

        val document = objectMapper.readValue(captured[1], UniqueIdDocument::class.java)
        assertEquals("E1234567890123abcd", document.id)
        assertTrue(document.creationDate.isNotBlank())
    }

    @Test
    fun `should propagate error when Redis fails`() {
        val redisError = RuntimeException("Redis connection error")

        doReturn(Uni.createFrom().failure<Response>(redisError))
            .whenever(redisDataSource)
            .execute(
                eq("SET"),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
            )

        val thrown =
            org.junit.jupiter.api.assertThrows<RuntimeException> {
                wrapper.saveIfAbsent("E1234567890123abcd").await().indefinitely()
            }

        assertTrue(thrown.message!!.contains("Redis connection error"))
    }
}
