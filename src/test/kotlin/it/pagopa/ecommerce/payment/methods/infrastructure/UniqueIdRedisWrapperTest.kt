package it.pagopa.ecommerce.payment.methods.infrastructure

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands
import io.quarkus.redis.datasource.value.ReactiveValueCommands
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UniqueIdRedisWrapperTest {

    private val valueCommands = mock<ReactiveValueCommands<String, String>>()
    private val keyCommands = mock<ReactiveKeyCommands<String>>()
    private val redisDataSource =
        mock<ReactiveRedisDataSource> {
            on { value(String::class.java, String::class.java) } doReturn valueCommands
            on { key() } doReturn keyCommands
        }

    private val wrapper = UniqueIdRedisWrapper(redisDataSource)

    @Test
    fun `should return true when key does not exist and is set successfully`() {
        val uniqueId = "E1234567890123abcd"
        val expectedKey = "uniqueId:$uniqueId"

        doReturn(Uni.createFrom().item(true))
            .whenever(valueCommands)
            .setnx(eq(expectedKey), eq(uniqueId))

        doReturn(Uni.createFrom().item(true)).whenever(keyCommands).expire(eq(expectedKey), eq(60L))

        val result = wrapper.saveIfAbsent(uniqueId).await().indefinitely()

        assertTrue(result)
        verify(valueCommands).setnx(expectedKey, uniqueId)
        verify(keyCommands).expire(expectedKey, 60L)
    }

    @Test
    fun `should return false when key already exists`() {
        val uniqueId = "E1234567890123abcd"
        val expectedKey = "uniqueId:$uniqueId"

        doReturn(Uni.createFrom().item(false))
            .whenever(valueCommands)
            .setnx(eq(expectedKey), eq(uniqueId))

        val result = wrapper.saveIfAbsent(uniqueId).await().indefinitely()

        assertFalse(result)
        verify(valueCommands).setnx(expectedKey, uniqueId)
        verify(keyCommands, never()).expire(any<String>(), any<Long>())
    }

    @Test
    fun `should propagate error when setnx fails`() {
        val uniqueId = "E1234567890123abcd"
        val expectedKey = "uniqueId:$uniqueId"
        val redisError = RuntimeException("Redis connection error")

        doReturn(Uni.createFrom().failure<Boolean>(redisError))
            .whenever(valueCommands)
            .setnx(eq(expectedKey), eq(uniqueId))

        val thrown =
            org.junit.jupiter.api.assertThrows<RuntimeException> {
                wrapper.saveIfAbsent(uniqueId).await().indefinitely()
            }

        assertTrue(thrown.message!!.contains("Redis connection error"))
        verify(keyCommands, never()).expire(any<String>(), any<Long>())
    }

    @Test
    fun `should propagate error when expire fails after successful setnx`() {
        val uniqueId = "E1234567890123abcd"
        val expectedKey = "uniqueId:$uniqueId"
        val expireError = RuntimeException("Expire failed")

        doReturn(Uni.createFrom().item(true))
            .whenever(valueCommands)
            .setnx(eq(expectedKey), eq(uniqueId))

        doReturn(Uni.createFrom().failure<Boolean>(expireError))
            .whenever(keyCommands)
            .expire(eq(expectedKey), eq(60L))

        val thrown =
            org.junit.jupiter.api.assertThrows<RuntimeException> {
                wrapper.saveIfAbsent(uniqueId).await().indefinitely()
            }

        assertTrue(thrown.message!!.contains("Expire failed"))
    }
}
