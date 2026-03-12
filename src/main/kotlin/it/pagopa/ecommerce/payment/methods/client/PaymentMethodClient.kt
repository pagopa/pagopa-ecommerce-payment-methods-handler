package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.NoBundleFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodNotFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.ecommerce.payment.methods.utils.BundleOptions
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
            .transform { error ->
                PaymentMethodsClientException(
                    "Error during the call to PaymentMethodsApi.searchPaymentMethods",
                    error,
                )
            }
    }

    private fun createGecFeeRequest(
        paymentMethodResponse: PaymentMethodResponseDto,
        feeRequestDto: CalculateFeeRequest,
    ): PaymentOptionMultiDto {

        val paymentNotices =
            feeRequestDto.paymentNotices
                .map { notice ->
                    val item =
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
                    log.info(
                        "Built PaymentNoticeItemDto: paymentAmount=[${item.paymentAmount}], " +
                            "primaryCreditorInstitution=[${item.primaryCreditorInstitution}], " +
                            "transferList size=[${item.transferList?.size}], " +
                            "transferList=[${item.transferList?.map {
                                "creditorInstitution=${it.creditorInstitution}, " +
                                        "transferCategory=${it.transferCategory}, " +
                                        "digitalStamp=${it.digitalStamp}"
                            }}]"
                    )
                    item
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
            this.bundles = sortAndShuffleBundleList(bundles)
            asset = paymentMethodResponse.paymentMethodAsset
            brandAssets = paymentMethodResponse.paymentMethodsBrandAssets
        }
    }

    private fun sortAndShuffleBundleList(bundles: List<Bundle>): List<Bundle> {
        // Separiamo il bundle onUs (se presente)
        val onUsBundle = bundles.firstOrNull { it.onUs == true }

        // Filtriamo gli altri, raggruppiamo per fee e mescoliamo
        val otherBundles =
            bundles
                .filter { it.onUs != true }
                .groupBy { it.taxPayerFee }
                .toSortedMap() // Mantiene l'ordinamento per fee (equivalente a TreeMap)
                .values
                .flatMap { bundlesPerFee -> bundlesPerFee.shuffled() }

        // Ritorna la lista con onUs in testa (se esistente) seguito dagli altri ordinati
        return listOfNotNull(onUsBundle) + otherBundles
    }

    fun calculateFees(
        paymentMethodsId: String,
        requestDto: CalculateFeeRequest,
        maxOccurrences: Int,
        xRequestId: String,
        xClientId: String,
        language: String,
    ): Uni<CalculateFeeResponse> {
        log.info(
            "calculateFees called for paymentMethodId: [$paymentMethodsId], xClientId: [$xClientId], requestDto: [$requestDto]"
        )

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
                log.info(
                    "getPaymentMethod response: group=[${res.group}], userTouchpoint=[${res.userTouchpoint}]"
                )

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
                val gecRequest = createGecFeeRequest(it, requestDto)
                log.info(
                    "Calling getFeesMulti with request: [$gecRequest], maxOccurrences: [$maxOccurrences], isAllCCP: [${requestDto.isAllCCP}]"
                )
                calculatorApi
                    .getFeesMulti(
                        gecRequest,
                        xRequestId,
                        maxOccurrences,
                        requestDto.isAllCCP.toString(),
                        null,
                        null,
                    )
                    .onFailure()
                    .invoke { exception ->
                        log.error(
                            "getFeesMulti failed with exception: [${exception.message}], gecRequest was: [$gecRequest]"
                        )
                        if (exception is ClientWebApplicationException) {
                            log.error(
                                "getFeesMulti response status: [${exception.response.status}], body: [${exception.response.readEntity(String::class.java)}]"
                            )
                        }
                    }
                    .map { bundle -> it to bundle }
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
                log.error("No bundle found for payment method [$paymentMethodsId]", error)
            }
    }

    fun getPaymentMethod(
        paymentMethodsId: String,
        xRequestId: String,
        xClientId: String,
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
    }
}
