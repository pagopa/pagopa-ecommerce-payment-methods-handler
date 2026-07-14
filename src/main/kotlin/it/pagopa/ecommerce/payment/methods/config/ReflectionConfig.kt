package it.pagopa.ecommerce.payment.methods.config

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection(
    classNames =
        [
            "it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson",
            "it.pagopa.generated.ecommerce.client.model.PaymentOptionMultiDto",
            "it.pagopa.ecommerce.payment.methods.v1.server.model.NpgSdkIntegrityResponse",
        ]
)
class ReflectionConfig {}
