package it.pagopa.ecommerce.payment.methods.services

import io.quarkus.cache.CacheResult
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.client.NpgClient
import it.pagopa.ecommerce.payment.methods.v1.server.model.NpgSdkIntegrityResponse
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*
import org.slf4j.LoggerFactory

@ApplicationScoped
class NpgSdkIntegrityService @Inject constructor(private val npgClient: NpgClient) {

    private val log = LoggerFactory.getLogger(NpgSdkIntegrityService::class.java)

    @CacheResult(cacheName = "npg-sdk-integrity")
    fun getIntegrityHash(): Uni<NpgSdkIntegrityResponse> {
        val correlationId = UUID.randomUUID()
        log.info("Fetching NPG SDK integrity hash with correlationId: $correlationId")
        return npgClient
            .getIntegrity(correlationId)
            .map { dto -> NpgSdkIntegrityResponse().apply { integrityHash = dto.integrity } }
            .onItem()
            .invoke { response ->
                log.info("NPG SDK integrity hash retrieved: ${response.integrityHash}")
            }
    }
}
