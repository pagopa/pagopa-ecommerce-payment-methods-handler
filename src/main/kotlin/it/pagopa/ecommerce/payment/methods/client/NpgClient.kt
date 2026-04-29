package it.pagopa.ecommerce.payment.methods.client

import com.fasterxml.jackson.annotation.JsonProperty

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

/** Request body for NPG order/build endpoint (CreateHostedOrderRequest). */
data class NpgBuildRequest(
    val version: String = "2",
    val merchantUrl: String,
    val order: NpgOrderDto,
    val paymentSession: NpgPaymentSessionDto,
)

data class NpgOrderDto(
    val orderId: String,
    val amount: String = "1",
    val currency: String = "EUR",
    val customerId: String? = null,
)

data class NpgPaymentSessionDto(
    val actionType: String = "PAY",
    val amount: String = "1",
    val language: String = "ITA",
    val paymentService: String,
    val resultUrl: String,
    val cancelUrl: String,
    val notificationUrl: String,
    val recurrence: RecurringSettingsDto? = null,
)

/** DTO representing NPG recurring payment settings. */
data class RecurringSettingsDto(
    val action: RecurringAction? = null,
    val contractId: String? = null,
    val contractType: RecurringContractType? = null,
    val contractExpiryDate: String? = null,
    val contractFrequency: String? = null,
)

/** Enum representing NPG recurring action types. */
enum class RecurringAction {
    NO_RECURRING,
    SUBSEQUENT_PAYMENT,
    CONTRACT_CREATION,
    CARD_SUBSTITUTION,
}

/** Enum representing NPG recurring contract types. */
enum class RecurringContractType {
    MIT_UNSCHEDULED,
    MIT_SCHEDULED,
    CIT,
}

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
    @JsonProperty("class") val propertyClass: String?,
    val src: String?,
)
