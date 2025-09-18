package it.pagopa.ecommerce.payment.methods.exception

class PaymentMethodNotFoundException : RuntimeException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)
}
