package it.pagopa.ecommerce.payment.methods.exception

class JwtIssuerResponseException(message: String, cause: Throwable) :
    RuntimeException(message, cause)
