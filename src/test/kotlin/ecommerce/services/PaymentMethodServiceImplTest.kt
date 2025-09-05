package it.pagopa.ecommerce.services

import ecommerce.client.PaymentMethodRestClient
import ecommerce.dto.PaymentMethod
import ecommerce.services.PaymentMethodServiceImpl
import io.smallrye.mutiny.Uni
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PaymentMethodServiceImplTest {

    private val restClient = mock<PaymentMethodRestClient>()
    private val service = PaymentMethodServiceImpl(restClient)

    @Test
    fun `getAll should return mocked payment methods`() {
        val expected = setOf(PaymentMethod().apply { paymentMethodID = "1" })
        Mockito.reset(restClient)
        whenever(restClient.getAll()).thenReturn(expected)

        val result = service.getAll()

        assertEquals(expected, result)
    }

    @Test
    fun `getAllAsync should return mocked result`() {
        val expected = PaymentMethod().apply { paymentMethodID = "10" }
        Mockito.reset(restClient)
        whenever(restClient.getAllAsync()).thenReturn(CompletableFuture.completedFuture(expected))

        val result = service.getAllAsync().toCompletableFuture().get()

        assertEquals("10", result.paymentMethodID)
    }

    @Test
    fun `getAllAsUni should return mocked result`() {
        val expected = PaymentMethod().apply { paymentMethodID = "20" }
        Mockito.reset(restClient)
        whenever(restClient.getAllAsUni()).thenReturn(Uni.createFrom().item(expected))

        val result = service.getAllAsUni().await().indefinitely()

        assertEquals("20", result.paymentMethodID)
    }

    @Test
    fun `getById should return mocked payment methods`() {
        val expected = setOf(PaymentMethod().apply { paymentMethodID = "99" })
        Mockito.reset(restClient)
        whenever(restClient.getById("99")).thenReturn(expected)

        val result = service.getById("99")

        assertEquals(expected, result)
    }

    @Test
    fun `getByIdAsync should return mocked result`() {
        val expected = setOf(PaymentMethod().apply { paymentMethodID = "42" })
        Mockito.reset(restClient)
        whenever(restClient.getByIdAsync("42"))
            .thenReturn(CompletableFuture.completedFuture(expected))

        val result = service.getByIdAsync("42").toCompletableFuture().get()

        assertEquals("42", result.first().paymentMethodID)
    }

    @Test
    fun `getByIdAsUni should return mocked result`() {
        val expected = setOf(PaymentMethod().apply { paymentMethodID = "77" })
        Mockito.reset(restClient)
        whenever(restClient.getByIdAsUni("77")).thenReturn(Uni.createFrom().item(expected))

        val result = service.getByIdAsUni("77").await().indefinitely()

        assertEquals("77", result.first().paymentMethodID)
    }
}
