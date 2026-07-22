package it.pagopa.ecommerce.payment.methods.exception

class SessionAlreadyAssociatedToTransactionException(
    orderId: String,
    existingTransactionId: String,
    requestedTransactionId: String,
) :
    RuntimeException(
        "Session for orderId [$orderId] is already associated to transaction [$existingTransactionId], " +
            "cannot associate to [$requestedTransactionId]"
    )
