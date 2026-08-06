package it.pagopa.ecommerce.payment.methods.exception

class UniqueIdGenerationException :
    RuntimeException("Unable to generate unique id after max retry attempts")
