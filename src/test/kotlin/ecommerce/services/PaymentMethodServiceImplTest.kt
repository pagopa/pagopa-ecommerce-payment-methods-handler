package it.pagopa.ecommerce.services

import ecommerce.client.PaymentMethodRestClient
import ecommerce.dto.PaymentMethod
import ecommerce.services.PaymentMethodServiceImpl
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
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
    fun `getByIdAsync should return mocked result`() {
        val expected = setOf(PaymentMethod().apply { paymentMethodID = "42" })
        Mockito.reset(restClient)
        whenever(restClient.getByIdAsync("42")).thenReturn(CompletableFuture.completedFuture(expected))

        val result = service.getByIdAsync("42").toCompletableFuture().get()

        assertEquals("42", result.first().paymentMethodID)
    }
}