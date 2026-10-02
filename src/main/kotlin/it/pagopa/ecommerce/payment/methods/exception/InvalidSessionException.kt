package it.pagopa.ecommerce.payment.methods.exception

class InvalidSessionException(orderId: String) :
    RuntimeException("Session for orderId [$orderId] has no associated transaction")
