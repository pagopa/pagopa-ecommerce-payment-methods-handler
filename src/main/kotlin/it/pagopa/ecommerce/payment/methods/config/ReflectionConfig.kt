package it.pagopa.ecommerce.payment.methods.config

import io.quarkus.runtime.annotations.RegisterForReflection
import it.pagopa.ecommerce.payment.methods.client.CreateTokenRequest
import it.pagopa.ecommerce.payment.methods.client.CreateTokenResponse
import it.pagopa.ecommerce.payment.methods.domain.CardDataDocument
import it.pagopa.ecommerce.payment.methods.domain.NpgSessionDocument
import it.pagopa.ecommerce.payment.methods.v1.server.model.CardFormFields
import it.pagopa.ecommerce.payment.methods.v1.server.model.CreateSessionResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.Field
import it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson
import it.pagopa.ecommerce.payment.methods.v1.server.model.SessionPaymentMethodResponse

@RegisterForReflection(
    targets =
        [
            ProblemJson::class,
            CreateSessionResponse::class,
            CardFormFields::class,
            Field::class,
            CreateTokenRequest::class,
            CreateTokenResponse::class,
            NpgSessionDocument::class,
            CardDataDocument::class,
            SessionPaymentMethodResponse::class,
        ]
)
class ReflectionConfig {}
