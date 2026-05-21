package it.pagopa.ecommerce.payment.methods.client

import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.NpgClientException
import it.pagopa.generated.ecommerce.npg.client.api.NpgBuildIntegrityApi
import it.pagopa.generated.ecommerce.npg.client.model.BuildIntegrityResponseDto
import java.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@QuarkusTest
class NpgClientTest {

    private val testCorrelationId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

    @Test
    fun `should return integrity hash on success`() {
        val expectedIntegrity =
            "sha384-dhR+dKVjiXoA8ZNODO+zKzgioWlnNMSKaLbNPT/GdTn0NgIoA4ke+4/bq6CfQjpJ"
        val mockApi = Mockito.mock(NpgBuildIntegrityApi::class.java)
        val mockResponse = BuildIntegrityResponseDto().apply { integrity = expectedIntegrity }

        whenever(mockApi.getBuildIntegrity(testCorrelationId))
            .thenReturn(Uni.createFrom().item(mockResponse))

        val client = NpgClient(mockApi)
        val result = client.getIntegrity(testCorrelationId).await().indefinitely()

        assertNotNull(result)
        assertEquals(expectedIntegrity, result.integrity)
    }

    @Test
    fun `should transform failure to NpgClientException`() {
        val mockApi = Mockito.mock(NpgBuildIntegrityApi::class.java)

        whenever(mockApi.getBuildIntegrity(testCorrelationId))
            .thenReturn(Uni.createFrom().failure(RuntimeException("Connection refused")))

        val client = NpgClient(mockApi)
        val exception =
            assertThrows(NpgClientException::class.java) {
                client.getIntegrity(testCorrelationId).await().indefinitely()
            }

        assertTrue(exception.message!!.contains("NpgBuildIntegrityApi.getBuildIntegrity"))
        assertNotNull(exception.cause)
    }
}
