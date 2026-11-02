package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.NpgClientException
import it.pagopa.ecommerce.payment.methods.utils.LogTracingUtils
import it.pagopa.generated.ecommerce.npg.client.api.NpgBuildIntegrityApi
import it.pagopa.generated.ecommerce.npg.client.model.BuildIntegrityResponseDto
import jakarta.enterprise.context.ApplicationScoped
import java.util.*
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class NpgClient(@param:RestClient private val npgBuildIntegrityApi: NpgBuildIntegrityApi) {

    private val log = LoggerFactory.getLogger(NpgClient::class.java)

    fun getIntegrity(correlationId: UUID): Uni<BuildIntegrityResponseDto> {
        return npgBuildIntegrityApi.getBuildIntegrity(correlationId).onFailure().transform { error
            ->
            LogTracingUtils.loggerTracingUtils()
                .failure()
                .dependency(LogTracingUtils.NPG_DEPENDENCY)
                .logWithStackTrace(log, error, "Error calling NPG Build Integrity API")
            NpgClientException(
                "Error during the call to NpgBuildIntegrityApi.getBuildIntegrity",
                error,
            )
        }
    }
}
