package it.pagopa.ecommerce.payment.methods.client

/**
 * Enum representing NPG payment methods. Maps the payment method name (as known by AFM/GMP) to the
 * NPG service name.
 */
enum class NpgPaymentMethod(val serviceName: String, val paymentTypeCode: String) {
    CARDS("CARDS", "CP");

    companion object {
        fun fromServiceName(name: String?): NpgPaymentMethod {
            return entries.firstOrNull { it.serviceName.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid NPG payment method: '$name'")
        }

        fun fromPaymentTypeCode(code: String?): NpgPaymentMethod {
            return entries.firstOrNull { it.paymentTypeCode.equals(code, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid NPG payment type code: '$code'")
        }
    }
}
