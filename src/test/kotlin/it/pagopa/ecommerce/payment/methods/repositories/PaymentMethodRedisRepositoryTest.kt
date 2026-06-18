package it.pagopa.ecommerce.payment.methods.repositories

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.redis.datasource.value.ReactiveValueCommands
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@QuarkusTest
class PaymentMethodRedisRepositoryTest {

    private val mockRedisDataSource = Mockito.mock(ReactiveRedisDataSource::class.java)
    private val mockCommands = Mockito.mock(ReactiveValueCommands::class.java) as ReactiveValueCommands<String, String>
    private var repository = PaymentMethodRedisRepository(mockRedisDataSource, 3600L)

    init {
        whenever(mockRedisDataSource.value(String::class.java)).thenReturn(mockCommands)
        repository = PaymentMethodRedisRepository(mockRedisDataSource, 3600L)
    }

    @Test
    fun `should return null on cache miss`() {
        whenever(mockCommands.get(anyOrNull())).thenReturn(Uni.createFrom().nullItem())

        val result = repository.findById("test-id").await().indefinitely()

        assertNull(result)
    }

    @Test
    fun `should return cached payment method on cache hit`() {
        val json = """{"id":"test-id","status":"ENABLED","paymentTypeCode":"CP","methodManagement":"ONBOARDABLE","name":{"IT":"Carte"},"description":{"IT":"Carte"},"paymentMethodAsset":"asset","paymentMethodTypes":["CARTE"],"validityDateFrom":"2025-01-01","paymentMethodsBrandAssets":{},"metadata":{}}"""

        whenever(mockCommands.get(anyOrNull())).thenReturn(Uni.createFrom().item(json))

        val result = repository.findById("test-id").await().indefinitely()

        assertNotNull(result)
        assertEquals("test-id", result?.id)
    }

    @Test
    fun `should save payment method to cache`() {
        val paymentMethod = PaymentMethodResponse().apply {
            id = "test-id"
            status = PaymentMethodResponse.StatusEnum.ENABLED
            paymentTypeCode = "CP"
            methodManagement = PaymentMethodResponse.MethodManagementEnum.ONBOARDABLE
            name = mapOf("IT" to "Carte")
            description = mapOf("IT" to "Carte")
            paymentMethodAsset = "asset"
            paymentMethodTypes = listOf(PaymentMethodResponse.PaymentMethodTypesEnum.CARTE)
            validityDateFrom = LocalDate.of(2025, 1, 1)
        }

        whenever(mockCommands.setex(anyOrNull(), anyOrNull<Long>(), anyOrNull()))
            .thenReturn(Uni.createFrom().voidItem())

        val result = repository.save(paymentMethod).await().indefinitely()

        assertNull(result)
    }
}