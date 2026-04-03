package it.pagopa.ecommerce.payment.methods.client

/**
 * Enum representing NPG payment methods. Maps the payment method name (as known by AFM/GMP) to the
 * NPG service name.
 */
enum class NpgPaymentMethod(val serviceName: String) {
    CARDS("CARDS");

    companion object {
        fun fromServiceName(name: String?): NpgPaymentMethod {
            return entries.firstOrNull { it.serviceName.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid NPG payment method: '$name'")
        }
    }
}

/** DTO representing NPG build form response fields. */
data class NpgFieldDto(
    val id: String?,
    val type: String?,
    val propertyClass: String?,
    val src: String?,
)

/** DTO representing NPG build form response. */
data class NpgFieldsDto(
    val sessionId: String,
    val securityToken: String,
    val fields: List<NpgFieldDto>,
)

/** Request body for NPG order/build endpoint. */
data class NpgBuildRequest(
    val merchantUrl: String,
    val resultUrl: String,
    val notificationUrl: String,
    val cancelUrl: String,
    val orderId: String,
    val paymentService: String,
    val amount: String? = null,
    val currency: String? = null,
    val customerId: String? = null,
    val language: String? = null,
)

/** Response from NPG order/build endpoint. */
data class NpgBuildResponse(
    val sessionId: String?,
    val securityToken: String?,
    val fields: List<NpgBuildFieldResponse>?,
    val state: String?,
)

data class NpgBuildFieldResponse(
    val id: String?,
    val type: String?,
    val propertyClass: String?,
    val src: String?,
)
