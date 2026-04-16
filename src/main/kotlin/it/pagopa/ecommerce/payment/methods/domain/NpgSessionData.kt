package it.pagopa.ecommerce.payment.methods.domain

data class CardDataDocument(
    val bin: String,
    val lastFourDigits: String,
    val expiringDate: String,
    val circuit: String,
)

data class NpgSessionDocument(
    val orderId: String,
    val correlationId: String,
    val sessionId: String,
    val securityToken: String,
    val cardData: CardDataDocument? = null,
    val transactionId: String? = null,
)
