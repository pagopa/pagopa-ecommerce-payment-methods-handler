package it.pagopa.ecommerce.payment.methods.client

import com.fasterxml.jackson.annotation.JsonCreator
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
data class NpgFieldDto
@JsonCreator
constructor(
    @JsonProperty("id") val id: String?,
    @JsonProperty("type") val type: String?,
    @JsonProperty("class") val propertyClass: String?,
    @JsonProperty("src") val src: String?,
)

/** DTO representing NPG build form response. */
data class NpgFieldsDto
@JsonCreator
constructor(
    @JsonProperty("sessionId") val sessionId: String,
    @JsonProperty("securityToken") val securityToken: String,
    @JsonProperty("fields") val fields: List<NpgFieldDto>,
)

/** Request body for NPG order/build endpoint (CreateHostedOrderRequest). */
data class NpgBuildRequest
@JsonCreator
constructor(
    @JsonProperty("version") val version: String = "2",
    @JsonProperty("merchantUrl") val merchantUrl: String,
    @JsonProperty("order") val order: NpgOrderDto,
    @JsonProperty("paymentSession") val paymentSession: NpgPaymentSessionDto,
)

data class NpgOrderDto
@JsonCreator
constructor(
    @JsonProperty("orderId") val orderId: String,
    @JsonProperty("amount") val amount: String = "1",
    @JsonProperty("currency") val currency: String = "EUR",
    @JsonProperty("customerId") val customerId: String? = null,
)

data class NpgPaymentSessionDto
@JsonCreator
constructor(
    @JsonProperty("actionType") val actionType: String = "PAY",
    @JsonProperty("amount") val amount: String = "1",
    @JsonProperty("language") val language: String = "ITA",
    @JsonProperty("paymentService") val paymentService: String,
    @JsonProperty("resultUrl") val resultUrl: String,
    @JsonProperty("cancelUrl") val cancelUrl: String,
    @JsonProperty("notificationUrl") val notificationUrl: String,
    @JsonProperty("recurrence") val recurrence: RecurringSettingsDto? = null,
)

/** DTO representing NPG recurring payment settings. */
data class RecurringSettingsDto
@JsonCreator
constructor(
    @JsonProperty("action") val action: RecurringAction? = null,
    @JsonProperty("contractId") val contractId: String? = null,
    @JsonProperty("contractType") val contractType: RecurringContractType? = null,
    @JsonProperty("contractExpiryDate") val contractExpiryDate: String? = null,
    @JsonProperty("contractFrequency") val contractFrequency: String? = null,
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
data class NpgBuildResponse
@JsonCreator
constructor(
    @JsonProperty("sessionId") val sessionId: String?,
    @JsonProperty("securityToken") val securityToken: String?,
    @JsonProperty("fields") val fields: List<NpgBuildFieldResponse>?,
    @JsonProperty("state") val state: String?,
)

data class NpgBuildFieldResponse
@JsonCreator
constructor(
    @JsonProperty("id") val id: String?,
    @JsonProperty("type") val type: String?,
    @JsonProperty("class") val propertyClass: String?,
    @JsonProperty("src") val src: String?,
)
