package it.pagopa.ecommerce.payment.methods.config

import io.quarkus.runtime.annotations.RegisterForReflection
import it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson

@RegisterForReflection(targets = [ProblemJson::class]) class ReflectionConfig {}
