package it.pagopa.ecommerce.payment.methods.resource.v1

import it.pagopa.ecommerce.payment.methods.exception.NpgClientException
import it.pagopa.ecommerce.payment.methods.services.NpgSdkIntegrityService
import it.pagopa.ecommerce.payment.methods.v1.server.api.NpgApi
import it.pagopa.ecommerce.payment.methods.v1.server.model.NpgSdkIntegrityResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.ProblemJson
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import java.util.concurrent.CompletionStage
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import org.slf4j.LoggerFactory

class NpgSdkIntegrityResource
@Inject
constructor(private val npgSdkIntegrityService: NpgSdkIntegrityService) : NpgApi {
    private val log = LoggerFactory.getLogger(NpgSdkIntegrityResource::class.java)

    override fun getNpgSdkIntegrity(): CompletionStage<NpgSdkIntegrityResponse> {
        return npgSdkIntegrityService.getIntegrityHash().subscribeAsCompletionStage()
    }

    @ServerExceptionMapper
    fun mapNpgClientException(exception: NpgClientException): RestResponse<ProblemJson> {
        log.error("NPG Client Exception while retrieving SDK integrity", exception)
        return problemResponse(
            Response.Status.INTERNAL_SERVER_ERROR,
            "NPG Communication Error",
            "Error retrieving NPG SDK integrity hash",
        )
    }

    @ServerExceptionMapper
    fun mapException(exception: Exception): RestResponse<ProblemJson> {
        log.error("Generic Exception while retrieving SDK integrity", exception)
        return problemResponse(
            Response.Status.INTERNAL_SERVER_ERROR,
            "Unexpected Exception",
            "Generic Error",
        )
    }

    private fun problemResponse(
        status: Response.Status,
        title: String,
        detail: String,
    ): RestResponse<ProblemJson> {
        val problem =
            ProblemJson().apply {
                this.status = status.statusCode
                this.title = title
                this.detail = detail
            }
        return RestResponse.status(status, problem)
    }
}
