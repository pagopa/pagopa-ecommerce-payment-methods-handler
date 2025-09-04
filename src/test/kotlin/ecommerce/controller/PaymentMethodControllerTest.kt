package it.pagopa.ecommerce.controller

import ecommerce.controller.PaymentMethodController
import ecommerce.dto.PaymentMethod
import ecommerce.services.PaymentMethodService
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import kotlin.test.*
import org.mockito.Mockito.`when`
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@QuarkusTest
class PaymentMethodControllerTest {

    @Inject
    lateinit var controller: PaymentMethodController

    @InjectMock
    lateinit var paymentMethodService: PaymentMethodService

    private fun mockPaymentMethod(id: String, name: String): PaymentMethod {
        val method = PaymentMethod()
        method.paymentMethodID = id
        method.paymentMethodName = name
        method.paymentMethodDescription = "desc"
        method.paymentMethodStatus = "ACTIVE"
        method.paymentMethodAsset = "asset.png"
        method.paymentMethodRanges = emptyList()
        method.paymentMethodTypeCode = "TYPE1"
        method.clientId = "client123"
        method.methodManagement = "AUTO"
        method.paymentMethodsBrandAssets = mapOf("brand" to "logo.png")
        return method
    }

    @Test
    fun testGetAllPaymentMethods() {
        val mockSet = setOf(mockPaymentMethod("1", "Card"), mockPaymentMethod("2", "PayPal"))
        `when`(paymentMethodService.getAll()).thenReturn(mockSet)

        val result = controller.getAllPaymentMethods()
        assertEquals(2, result.size)
        assertTrue(result.any { it.paymentMethodName == "Card" })
    }

    @Test
    fun testGetAllPaymentMethodsAsync() {
        val mock = mockPaymentMethod("1", "Card")
        `when`(paymentMethodService.getAllAsync()).thenReturn(CompletableFuture.completedFuture(mock))

        val result: CompletionStage<PaymentMethod> = controller.getAllPaymentMethodsAsync()
        assertEquals("Card", result.toCompletableFuture().join().paymentMethodName)
    }

    @Test
    fun testGetAllPaymentMethodsAsUni() {
        val mock = mockPaymentMethod("1", "Card")
        `when`(paymentMethodService.getAllAsUni()).thenReturn(Uni.createFrom().item(mock))

        val result: Uni<PaymentMethod> = controller.getAllPaymentMethodsAsUni()
        assertEquals("Card", result.await().indefinitely().paymentMethodName)
    }

    @Test
    fun testGetPaymentMethodById() {
        val mockSet = setOf(mockPaymentMethod("1", "Card"))
        `when`(paymentMethodService.getById("123")).thenReturn(mockSet)

        val result = controller.getPaymentMethod("123")
        assertEquals(1, result.size)
        assertEquals("Card", result.first().paymentMethodName)
    }

    @Test
    fun testGetPaymentMethodAsync() {
        val mockSet = setOf(mockPaymentMethod("1", "Card"))
        `when`(paymentMethodService.getByIdAsync("123")).thenReturn(CompletableFuture.completedFuture(mockSet))

        val result: CompletionStage<Set<PaymentMethod>> = controller.getPaymentMethodAsync("123")
        assertEquals(1, result.toCompletableFuture().join().size)
    }

    @Test
    fun testGetPaymentMethodMutiny() {
        val mockSet = setOf(mockPaymentMethod("1", "Card"))
        `when`(paymentMethodService.getByIdAsUni("123")).thenReturn(Uni.createFrom().item(mockSet))

        val result: Uni<Set<PaymentMethod>> = controller.getPaymentMethodMutiny("123")
        assertEquals(1, result.await().indefinitely().size)
    }
}
