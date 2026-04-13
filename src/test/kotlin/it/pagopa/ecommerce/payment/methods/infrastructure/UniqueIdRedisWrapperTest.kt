package it.pagopa.ecommerce.payment.methods.infrastructure

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.redis.datasource.value.ReactiveValueCommands
import io.quarkus.redis.datasource.value.SetArgs
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UniqueIdRedisWrapperTest {

    private val valueCommands = mock<ReactiveValueCommands<String, String>>()
    private val redisDataSource =
        mock<ReactiveRedisDataSource> {
            on { value(String::class.java, String::class.java) } doReturn valueCommands
        }

    private val wrapper = UniqueIdRedisWrapper(redisDataSource)

    @Test
    fun `should return true when key does not exist and is set successfully`() {
        val uniqueId = "E1234567890123abcd"
        val expectedKey = "uniqueId:$uniqueId"

        // setGet returns null when key was newly set (no previous value)
        doReturn(Uni.createFrom().nullItem<String>())
            .whenever(valueCommands)
            .setGet(eq(expectedKey), eq(uniqueId), any<SetArgs>())

        val result = wrapper.saveIfAbsent(uniqueId).await().indefinitely()

        assertTrue(result)
        verify(valueCommands).setGet(eq(expectedKey), eq(uniqueId), any<SetArgs>())
    }

    @Test
    fun `should return false when key already exists`() {
        val uniqueId = "E1234567890123abcd"
        val expectedKey = "uniqueId:$uniqueId"

        // setGet returns the existing value when key already exists
        doReturn(Uni.createFrom().item(uniqueId))
            .whenever(valueCommands)
            .setGet(eq(expectedKey), eq(uniqueId), any<SetArgs>())

        val result = wrapper.saveIfAbsent(uniqueId).await().indefinitely()

        assertFalse(result)
    }

    @Test
    fun `should propagate error when Redis fails`() {
        val uniqueId = "E1234567890123abcd"
        val expectedKey = "uniqueId:$uniqueId"
        val redisError = RuntimeException("Redis connection error")

        doReturn(Uni.createFrom().failure<String>(redisError))
            .whenever(valueCommands)
            .setGet(eq(expectedKey), eq(uniqueId), any<SetArgs>())

        val thrown =
            org.junit.jupiter.api.assertThrows<RuntimeException> {
                wrapper.saveIfAbsent(uniqueId).await().indefinitely()
            }

        assertTrue(thrown.message!!.contains("Redis connection error"))
    }
}
