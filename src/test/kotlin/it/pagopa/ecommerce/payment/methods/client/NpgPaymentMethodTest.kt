package it.pagopa.ecommerce.payment.methods.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NpgPaymentMethodTest {

    @Test
    fun `should resolve CARDS from service name`() {
        assertEquals(NpgPaymentMethod.CARDS, NpgPaymentMethod.fromServiceName("CARDS"))
    }

    @Test
    fun `should resolve CARDS case insensitive`() {
        assertEquals(NpgPaymentMethod.CARDS, NpgPaymentMethod.fromServiceName("cards"))
        assertEquals(NpgPaymentMethod.CARDS, NpgPaymentMethod.fromServiceName("Cards"))
    }

    @Test
    fun `should throw IllegalArgumentException for unknown payment method`() {
        val thrown =
            assertThrows<IllegalArgumentException> { NpgPaymentMethod.fromServiceName("UNKNOWN") }
        assertEquals("Invalid NPG payment method: 'UNKNOWN'", thrown.message)
    }

    @Test
    fun `should throw IllegalArgumentException for null payment method`() {
        val thrown =
            assertThrows<IllegalArgumentException> { NpgPaymentMethod.fromServiceName(null) }
        assertEquals("Invalid NPG payment method: 'null'", thrown.message)
    }
}
