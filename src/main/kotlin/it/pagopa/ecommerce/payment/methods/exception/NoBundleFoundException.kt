package it.pagopa.ecommerce.payment.methods.exception

class NoBundleFoundException : RuntimeException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)
}
