package it.pagopa.ecommerce.payment.methods.services

import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.ecommerce.payment.methods.v1.server.model.FeeRange
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentNoticeItemDto
import it.pagopa.generated.ecommerce.client.model.TransferListItemDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.concurrent.CompletionStage
import org.slf4j.LoggerFactory

@ApplicationScoped
class PaymentMethodServiceImpl @Inject constructor(private val restClient: PaymentMethodsClient) :
    PaymentMethodService {

    private val log = LoggerFactory.getLogger(PaymentMethodServiceImpl::class.java)

    override fun searchPaymentMethods(
        paymentMethodsRequest: PaymentMethodsRequest,
        xRequestId: String,
    ): CompletionStage<PaymentMethodsResponse> {
        return restClient
            .searchPaymentMethods(buildAfmRequest(paymentMethodsRequest), xRequestId)
            .map { dto -> afmResponseToPaymentHandlerResponse(dto) }
            .onFailure()
            .invoke { exception ->
                log.error("Exception during request with id $xRequestId", exception)
            }
            .onItem()
            .invoke { _ ->
                log.info("Payment methods retrieved successfully for request with id $xRequestId")
            }
            .subscribeAsCompletionStage()
    }

    private fun buildAfmRequest(
        paymentHandlerRequest: PaymentMethodsRequest
    ): PaymentMethodRequestDto {
        val afmRequest = PaymentMethodRequestDto()
        afmRequest.userTouchpoint =
            PaymentMethodRequestDto.UserTouchpointEnum.valueOf(
                paymentHandlerRequest.userTouchpoint.toString()
            )
        afmRequest.userDevice =
            paymentHandlerRequest.userDevice?.let { device ->
                PaymentMethodRequestDto.UserDeviceEnum.valueOf(device.toString())
            }
        afmRequest.totalAmount = paymentHandlerRequest.totalAmount
        paymentHandlerRequest.paymentNotice.forEach { notice ->
            val paymentNotice = PaymentNoticeItemDto()
            paymentNotice.paymentAmount = notice.paymentAmount
            paymentNotice.primaryCreditorInstitution = notice.primaryCreditorInstitution
            notice.transferList?.forEach { transfer ->
                val transferListItem = TransferListItemDto()
                transferListItem.creditorInstitution = transfer.creditorInstitution
                transferListItem.transferCategory = transfer.transferCategory
                transferListItem.digitalStamp = transfer.digitalStamp

                paymentNotice.transferList.add(transferListItem)
            }

            afmRequest.paymentNotice.add(paymentNotice)
        }
        afmRequest.allCCp = paymentHandlerRequest.allCCp
        afmRequest.targetKey = paymentHandlerRequest.targetKey

        return afmRequest
    }

    private fun afmResponseToPaymentHandlerResponse(
        afmResponse: PaymentMethodsResponseDto
    ): PaymentMethodsResponse {
        val paymentHandlerResponse = PaymentMethodsResponse()

        afmResponse.paymentMethods.forEach { afmResponsePaymentMethod ->
            val paymentHandlerPaymentMethod = PaymentMethodResponse()

            paymentHandlerPaymentMethod.id = afmResponsePaymentMethod.paymentMethodId
            paymentHandlerPaymentMethod.name = afmResponsePaymentMethod.name
            paymentHandlerPaymentMethod.description = afmResponsePaymentMethod.description
            paymentHandlerPaymentMethod.status =
                PaymentMethodResponse.StatusEnum.valueOf(afmResponsePaymentMethod.status.toString())
            paymentHandlerPaymentMethod.feeRange =
                afmResponsePaymentMethod.feeRange?.let { afmResponseFeeRange ->
                    FeeRange().max(afmResponseFeeRange.max).min(afmResponseFeeRange.min)
                }
            paymentHandlerPaymentMethod.paymentTypeCode =
                PaymentMethodResponse.PaymentTypeCodeEnum.valueOf(
                    afmResponsePaymentMethod.group.toString()
                )
            paymentHandlerPaymentMethod.paymentMethodAsset =
                afmResponsePaymentMethod.paymentMethodAsset
            paymentHandlerPaymentMethod.methodManagement =
                PaymentMethodResponse.MethodManagementEnum.valueOf(
                    afmResponsePaymentMethod.methodManagement.toString()
                )
            paymentHandlerPaymentMethod.paymentMethodsBrandAssets =
                afmResponsePaymentMethod.paymentMethodsBrandAssets
            paymentHandlerPaymentMethod.validityDateFrom = afmResponsePaymentMethod.validityDateFrom
            paymentHandlerPaymentMethod.disabledReason =
                afmResponsePaymentMethod.disabledReason?.let {
                    PaymentMethodResponse.DisabledReasonEnum.valueOf(
                        afmResponsePaymentMethod.disabledReason.toString()
                    )
                }
            paymentHandlerPaymentMethod.metadata = afmResponsePaymentMethod.metadata
            paymentHandlerPaymentMethod.paymentMethodTypes =
                afmResponsePaymentMethod.paymentMethodTypes?.let {
                    afmResponsePaymentMethod.paymentMethodTypes.map { payMethodType ->
                        PaymentMethodResponse.PaymentMethodTypesEnum.valueOf(
                            payMethodType.toString()
                        )
                    }
                }

            paymentHandlerResponse.addPaymentMethodsItem(paymentHandlerPaymentMethod)
        }

        return paymentHandlerResponse
    }
}
