package it.pagopa.ecommerce.payment.methods.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class CardDataDocument
@JsonCreator
constructor(
    @JsonProperty("bin") val bin: String,
    @JsonProperty("lastFourDigits") val lastFourDigits: String,
    @JsonProperty("expiringDate") val expiringDate: String,
    @JsonProperty("circuit") val circuit: String,
)

data class NpgSessionDocument
@JsonCreator
constructor(
    @JsonProperty("orderId") val orderId: String,
    @JsonProperty("correlationId") val correlationId: String,
    @JsonProperty("sessionId") val sessionId: String,
    @JsonProperty("securityToken") val securityToken: String,
    @JsonProperty("cardData") val cardData: CardDataDocument? = null,
    @JsonProperty("transactionId") val transactionId: String? = null,
)
