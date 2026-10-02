package it.pagopa.ecommerce.payment.methods.exception

class MismatchedSecurityTokenException(orderId: String, transactionId: String) :
    RuntimeException(
        "Invalid security token for orderId [$orderId], transactionId [$transactionId]"
    )
