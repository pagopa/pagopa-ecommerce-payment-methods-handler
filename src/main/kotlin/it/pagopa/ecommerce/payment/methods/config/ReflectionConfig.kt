package it.pagopa.ecommerce.payment.methods.config

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection(
    classNames =
        [
            "it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson",
            "it.pagopa.generated.ecommerce.client.model.PaymentNoticeItemDto",
            "it.pagopa.generated.ecommerce.client.model.TransferListItemDto",
            "it.pagopa.generated.ecommerce.client.model.PaymentOptionMultiDto",
            "it.pagopa.generated.ecommerce.client.model.PspSearchCriteriaDto",
        ]
)
class ReflectionConfig {}
