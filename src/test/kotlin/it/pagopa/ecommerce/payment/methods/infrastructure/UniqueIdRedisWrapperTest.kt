package it.pagopa.ecommerce.payment.methods.infrastructure

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.redis.client.Response
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UniqueIdRedisWrapperTest {

    private val redisDataSource = mock<ReactiveRedisDataSource>()
    private val wrapper = UniqueIdRedisWrapper(redisDataSource)

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
