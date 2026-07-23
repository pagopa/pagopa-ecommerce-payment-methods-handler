package it.pagopa.ecommerce.payment.methods.utils

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.UniqueIdGenerationException
import it.pagopa.ecommerce.payment.methods.infrastructure.UniqueIdRedisWrapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

class UniqueIdGeneratorTest {

    private val uniqueIdRedisWrapper = mock(UniqueIdRedisWrapper::class.java)
    private val uniqueIdGenerator = UniqueIdGenerator(uniqueIdRedisWrapper)

    @Test
    fun `should generate unique id without retry`() {
        doReturn(Uni.createFrom().item(true)).whenever(uniqueIdRedisWrapper).saveIfAbsent(any())

        val result = uniqueIdGenerator.generateUniqueId().await().indefinitely()

        assertEquals(UniqueIdGenerator.MAX_LENGTH, result.length)
        assertTrue(result.startsWith(UniqueIdGenerator.PRODUCT_PREFIX))
        verify(uniqueIdRedisWrapper, times(1)).saveIfAbsent(any())
    }

    @Test
    fun `should generate unique id with retry on collision`() {
        var callCount = 0
        whenever(uniqueIdRedisWrapper.saveIfAbsent(any())).thenAnswer {
            callCount++
            if (callCount < 3) {
                Uni.createFrom().item(false)
            } else {
                Uni.createFrom().item(true)
            }
        }

        val result = uniqueIdGenerator.generateUniqueId().await().indefinitely()

        assertEquals(UniqueIdGenerator.MAX_LENGTH, result.length)
        assertTrue(result.startsWith(UniqueIdGenerator.PRODUCT_PREFIX))
        verify(uniqueIdRedisWrapper, times(3)).saveIfAbsent(any())
    }

    @Test
    fun `should throw UniqueIdGenerationException when all attempts are exhausted`() {
        doReturn(Uni.createFrom().item(false)).whenever(uniqueIdRedisWrapper).saveIfAbsent(any())

        assertThrows<UniqueIdGenerationException> {
            uniqueIdGenerator.generateUniqueId().await().indefinitely()
        }

        verify(uniqueIdRedisWrapper, times(UniqueIdGenerator.MAX_ATTEMPTS)).saveIfAbsent(any())
    }

    @Test
    fun `should log error and propagate failure when Redis throws unexpected exception`() {
        val redisError = RuntimeException("Redis connection refused")

        whenever(uniqueIdRedisWrapper.saveIfAbsent(any()))
            .thenReturn(Uni.createFrom().failure(redisError))

        val thrown =
            assertThrows<RuntimeException> {
                uniqueIdGenerator.generateUniqueId().await().indefinitely()
            }

        assertEquals("Redis connection refused", thrown.message)
        verify(uniqueIdRedisWrapper, times(1)).saveIfAbsent(any())
    }

    @Test
    fun `should generate different ids on each attempt when exhausted`() {
        val captor = argumentCaptor<String>()

        doReturn(Uni.createFrom().item(false))
            .whenever(uniqueIdRedisWrapper)
            .saveIfAbsent(captor.capture())

        assertThrows<UniqueIdGenerationException> {
            uniqueIdGenerator.generateUniqueId().await().indefinitely()
        }

        verify(uniqueIdRedisWrapper, times(UniqueIdGenerator.MAX_ATTEMPTS)).saveIfAbsent(any())

        val savedIds = captor.allValues.toSet()
        assertEquals(
            UniqueIdGenerator.MAX_ATTEMPTS,
            savedIds.size,
            "saved ids: $savedIds, expected ${UniqueIdGenerator.MAX_ATTEMPTS} different values",
        )
    }
}
