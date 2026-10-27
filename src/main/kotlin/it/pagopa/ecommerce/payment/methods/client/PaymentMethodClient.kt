package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.NoBundleFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodNotFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.utils.BundleOptions
import it.pagopa.ecommerce.payment.methods.utils.LogTracingUtils
import it.pagopa.ecommerce.payment.methods.v1.server.model.Bundle
import it.pagopa.ecommerce.payment.methods.v1.server.model.CalculateFeeRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.CalculateFeeResponse
import it.pagopa.generated.ecommerce.client.api.CalculatorApi
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.BundleOptionDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentNoticeItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentOptionMultiDto
import it.pagopa.generated.ecommerce.client.model.PspSearchCriteriaDto
import it.pagopa.generated.ecommerce.client.model.TransferListItemDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.ClientWebApplicationException
import org.slf4j.LoggerFactory

@ApplicationScoped
class PaymentMethodsClient(
    @param:RestClient private val paymentMethodsApi: PaymentMethodsApi,
    @param:RestClient private val calculatorApi: CalculatorApi,
) {

    private val log = LoggerFactory.getLogger(PaymentMethodsClient::class.java)

    fun searchPaymentMethods(
        requestDto: PaymentMethodRequestDto,
        xRequestId: String,
    ): Uni<PaymentMethodsResponseDto> {

        return paymentMethodsApi
            .searchPaymentMethods(requestDto, xRequestId)
            .onFailure()
            .invoke { error ->
                LogTracingUtils.loggerTracingUtils()
                    .failure()
                    .details(
                        mapOf(
                            "userTouchpoint" to requestDto.userTouchpoint.toString(),
                            "userDevice" to requestDto.userDevice.toString(),
                            "paymentNoticesCount" to requestDto.paymentNotice.size.toString(),
                        )
                    )
                    .dependency(LogTracingUtils.AFM_DEPENDENCY)
                    .logError(
                        log,
                        error,
                        "Error during the call to PaymentMethodsApi.searchPaymentMethods",
                    )
            }
            .onFailure()
            .transform { error ->
                PaymentMethodsClientException(
                    "Error during the call to PaymentMethodsApi.searchPaymentMethods",
                    error,
                )
            }
            .onItem()
            .invoke { _ ->
                LogTracingUtils.loggerTracingUtils()
                    .success()
                    .details(
                        mapOf(
                            "userTouchpoint" to requestDto.userTouchpoint.toString(),
                            "userDevice" to requestDto.userDevice.toString(),
                            "paymentNoticesCount" to requestDto.paymentNotice.size.toString(),
                        )
                    )
                    .dependency(LogTracingUtils.AFM_DEPENDENCY)
                    .logInfo(log, "Payment methods retrieved successfully!")
            }
    }

    private fun createGecFeeRequest(
        paymentMethodResponse: PaymentMethodResponseDto,
        feeRequestDto: CalculateFeeRequest,
    ): PaymentOptionMultiDto {

        val paymentNotices =
            feeRequestDto.paymentNotices
                .map { notice ->
                    PaymentNoticeItemDto().apply {
                        paymentAmount = notice.paymentAmount
                        primaryCreditorInstitution = notice.primaryCreditorInstitution
                        transferList =
                            notice.transferList.map { t ->
                                TransferListItemDto().apply {
                                    creditorInstitution = t.creditorInstitution
                                    digitalStamp = t.digitalStamp
                                    transferCategory = t.transferCategory
                                }
                            }
                    }
                }
                .toList()

        return PaymentOptionMultiDto().apply {
            bin = feeRequestDto.bin
            idPspList =
                feeRequestDto.idPspList?.map { PspSearchCriteriaDto().apply { idPsp = it } }
                    ?: emptyList()
            paymentMethod = paymentMethodResponse.group
            touchpoint = feeRequestDto.touchpoint
            paymentNotice = paymentNotices
        }
    }

    private fun removeDuplicatePspV2(bundle: BundleOptionDto): BundleOptionDto {
        return BundleOptions.removeDuplicatePsp(bundle)
    }

    private fun bundleOptionToResponse(
        paymentMethodResponse: PaymentMethodResponseDto,
        bundle: BundleOptionDto,
        language: String,
    ): CalculateFeeResponse {
        val bundles =
            (bundle.bundleOptions ?: emptyList()).map { t ->
                Bundle().apply {
                    abi = t.abi
                    bundleDescription = t.bundleDescription
                    bundleName = t.bundleName
                    idBrokerPsp = t.idBrokerPsp
                    idBundle = t.idBundle
                    idChannel = t.idChannel
                    idPsp = t.idPsp
                    onUs = t.onUs
                    // Se t.paymentMethod è null, usa il valore del documento (AFM domain "any")
                    paymentMethod = t.paymentMethod ?: paymentMethodResponse.group
                    taxPayerFee = t.taxPayerFee
                    touchpoint = t.touchpoint
                    pspBusinessName = t.pspBusinessName
                }
            }

        return CalculateFeeResponse().apply {
            belowThreshold = bundle.belowThreshold
            paymentMethodName = paymentMethodResponse.name[language]
            paymentMethodDescription = paymentMethodResponse.description[language]
            paymentMethodStatus =
                CalculateFeeResponse.PaymentMethodStatusEnum.valueOf(
                    paymentMethodResponse.status.name
                )
            this.bundles = bundles
            asset = paymentMethodResponse.paymentMethodAsset
            brandAssets = paymentMethodResponse.paymentMethodsBrandAssets
        }
    }

    fun calculateFees(
        paymentMethodsId: String,
        requestDto: CalculateFeeRequest,
        maxOccurrences: Int,
        xRequestId: String,
        xClientId: String,
        language: String,
    ): Uni<CalculateFeeResponse> {
        return paymentMethodsApi
            .getPaymentMethod(paymentMethodsId, xRequestId)
            .onFailure()
            .invoke { error ->
                LogTracingUtils.loggerTracingUtils()
                    .failure()
                    .details(mapOf("paymentMethodId" to paymentMethodsId))
                    .dependency(LogTracingUtils.AFM_DEPENDENCY)
                    .logWithStackTrace(
                        log,
                        error,
                        "Error during the call to PaymentMethodsApi.calculateFees",
                    )
            }
            .onFailure()
            .transform { exception ->
                if (
                    exception is ClientWebApplicationException &&
                        exception.response.status == Response.Status.NOT_FOUND.statusCode
                ) {
                    PaymentMethodNotFoundException(
                        "Payment method $paymentMethodsId not found",
                        exception,
                    )
                } else {
                    PaymentMethodsClientException(
                        "Error during the call to PaymentMethodsApi.calculateFees",
                        exception,
                    )
                }
            }
            .onItem()
            .invoke { res ->
                LogTracingUtils.loggerTracingUtils()
                    .success()
                    .details(mapOf("paymentMethodId" to paymentMethodsId))
                    .dependency(LogTracingUtils.AFM_DEPENDENCY)
                    .logInfo(log, "calculateFees successfully!")
                if (
                    !res.userTouchpoint.contains(
                        PaymentMethodResponseDto.UserTouchpointEnum.valueOf(xClientId)
                    )
                ) {
                    throw PaymentMethodNotFoundException(
                        "Payment method $paymentMethodsId not found for client id $xClientId"
                    )
                }
            }
            .flatMap {
                calculatorApi
                    .getFeesMulti(
                        createGecFeeRequest(it, requestDto),
                        xRequestId,
                        maxOccurrences,
                        requestDto.isAllCCP.toString(),
                        "true",
                        "feerandom",
                    )
                    .map { bundle -> it to bundle }
                    .onItem()
                    .invoke { _ ->
                        LogTracingUtils.loggerTracingUtils()
                            .success()
                            .details(
                                mapOf(
                                    "paymentMethodId" to paymentMethodsId,
                                    "maxOccurrences" to maxOccurrences.toString(),
                                    "isAllCCP" to requestDto.isAllCCP.toString(),
                                )
                            )
                            .dependency(LogTracingUtils.AFM_DEPENDENCY)
                            .logInfo(log, "CalculatorApi.getFeesMulti successfully!")
                    }
                    .onFailure()
                    .invoke { error ->
                        LogTracingUtils.loggerTracingUtils()
                            .failure()
                            .details(
                                mapOf(
                                    "paymentMethodId" to paymentMethodsId,
                                    "maxOccurrences" to maxOccurrences.toString(),
                                    "isAllCCP" to requestDto.isAllCCP.toString(),
                                )
                            )
                            .dependency(LogTracingUtils.AFM_DEPENDENCY)
                            .logWithStackTrace(
                                log,
                                error,
                                "Error during the call to CalculatorApi.getFeesMulti",
                            )
                    }
            }
            .map { (p, b) ->
                val bundles = removeDuplicatePspV2(b)
                bundleOptionToResponse(p, bundles, language)
            }
            .onItem()
            .invoke { res ->
                if (res.bundles.isEmpty()) {
                    throw NoBundleFoundException("")
                }
            }
            .onFailure(NoBundleFoundException::class.java)
            .invoke { error ->
                LogTracingUtils.loggerTracingUtils()
                    .failure()
                    .details(mapOf("paymentMethodId" to paymentMethodsId))
                    .dependency(LogTracingUtils.AFM_DEPENDENCY)
                    .logWithStackTrace(log, error, "No bundle found for this payment method")
            }
    }

    fun getPaymentMethod(
        paymentMethodsId: String,
        xRequestId: String,
        xClientId: String?,
    ): Uni<PaymentMethodResponseDto> {
        return paymentMethodsApi
            .getPaymentMethod(paymentMethodsId, xRequestId)
            .onFailure()
            .transform { exception ->
                if (
                    exception is ClientWebApplicationException &&
                        exception.response.status == Response.Status.NOT_FOUND.statusCode
                ) {
                    PaymentMethodNotFoundException(
                        "Payment method $paymentMethodsId not found",
                        exception,
                    )
                } else {
                    PaymentMethodsClientException(
                        "Error during the call to PaymentMethodsApi.getPaymentMethod",
                        exception,
                    )
                }
            }
            .onItem()
            .invoke { res ->
                if (xClientId != null) {
                    val touchpoint =
                        runCatching {
                                PaymentMethodResponseDto.UserTouchpointEnum.valueOf(xClientId)
                            }
                            .getOrNull()
                    if (touchpoint == null || !res.userTouchpoint.contains(touchpoint)) {
                        throw PaymentMethodNotFoundException(
                            "Payment method $paymentMethodsId not found for client id $xClientId"
                        )
                    }
                }
            }
    }
}
