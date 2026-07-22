package it.pagopa.ecommerce.payment.methods.exception

class OrderIdNotFoundException(orderId: String) : RuntimeException("Order id not found: $orderId")
