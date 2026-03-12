package it.pagopa.ecommerce.payment.methods.config

import io.quarkus.runtime.annotations.RegisterForReflection
import it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson
import it.pagopa.generated.ecommerce.client.model.PaymentNoticeItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentOptionMultiDto
import it.pagopa.generated.ecommerce.client.model.PspSearchCriteriaDto
import it.pagopa.generated.ecommerce.client.model.TransferListItemDto

@RegisterForReflection(
    targets =
        [
            ProblemJson::class,
            PaymentNoticeItemDto::class,
            TransferListItemDto::class,
            PaymentOptionMultiDto::class,
            PspSearchCriteriaDto::class,
        ]
)
class ReflectionConfig {}
