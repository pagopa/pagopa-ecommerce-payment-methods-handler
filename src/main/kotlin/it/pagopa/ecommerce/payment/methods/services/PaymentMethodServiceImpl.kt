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
            .subscribeAsCompletionStage()
    }

    private fun buildAfmRequest(
        paymentMethodsRequest: PaymentMethodsRequest
    ): PaymentMethodRequestDto {
        val paymentRequestDto = PaymentMethodRequestDto()
        paymentRequestDto.userTouchpoint =
            PaymentMethodRequestDto.UserTouchpointEnum.valueOf(
                paymentMethodsRequest.userTouchpoint.toString()
            )
        paymentRequestDto.userDevice =
            paymentMethodsRequest.userDevice?.let { device ->
                PaymentMethodRequestDto.UserDeviceEnum.valueOf(device.toString())
            }
        paymentRequestDto.totalAmount = paymentMethodsRequest.totalAmount
        paymentMethodsRequest.paymentNotice.forEach { notice ->
            val paymentNotice = PaymentNoticeItemDto()
            paymentNotice.paymentAmount = notice.paymentAmount
            paymentNotice.primaryCreditorInstitution = notice.primaryCreditorInstitution
            notice.transferList?.forEach { transfer ->
                val transferListItem = TransferListItemDto()
                transferListItem.creditorInstitution = transfer.creditorInstitution
                paymentNotice.transferList.add(transferListItem)
            }

            paymentRequestDto.paymentNotice.add(paymentNotice)
        }
        paymentRequestDto.allCCp = paymentMethodsRequest.allCCp
        paymentRequestDto.targetKey = paymentMethodsRequest.targetKey

        return paymentRequestDto
    }

    private fun afmResponseToPaymentHandlerResponse(
        paymentMethodsResponseDto: PaymentMethodsResponseDto
    ): PaymentMethodsResponse {
        val paymentMethodsResponse = PaymentMethodsResponse()

        paymentMethodsResponseDto.paymentMethods.forEach { gmpPaymentMethod ->
            val paymentMethod = PaymentMethodResponse()

            paymentMethod.id = gmpPaymentMethod.paymentMethodId
            paymentMethod.name = gmpPaymentMethod.name
            paymentMethod.description = gmpPaymentMethod.description
            paymentMethod.status =
                PaymentMethodResponse.StatusEnum.valueOf(gmpPaymentMethod.status.toString())
            paymentMethod.feeRange =
                gmpPaymentMethod.feeRange?.let { gmpFeeRange ->
                    FeeRange().max(gmpFeeRange.max).min(gmpFeeRange.min)
                }
            paymentMethod.paymentTypeCode =
                PaymentMethodResponse.PaymentTypeCodeEnum.valueOf(gmpPaymentMethod.group.toString())
            paymentMethod.paymentMethodAsset = gmpPaymentMethod.paymentMethodAsset
            paymentMethod.methodManagement =
                PaymentMethodResponse.MethodManagementEnum.valueOf(
                    gmpPaymentMethod.methodManagement.toString()
                )
            paymentMethod.paymentMethodsBrandAssets = gmpPaymentMethod.paymentMethodsBrandAssets
            paymentMethod.validityDateFrom = gmpPaymentMethod.validityDateFrom
            paymentMethod.disabledReason =
                gmpPaymentMethod.disabledReason?.let {
                    PaymentMethodResponse.DisabledReasonEnum.valueOf(
                        gmpPaymentMethod.disabledReason.toString()
                    )
                }
            paymentMethod.metadata = gmpPaymentMethod.metadata
            paymentMethod.paymentMethodTypes =
                gmpPaymentMethod.paymentMethodTypes?.let {
                    gmpPaymentMethod.paymentMethodTypes.map { payMethodType ->
                        PaymentMethodResponse.PaymentMethodTypesEnum.valueOf(
                            payMethodType.toString()
                        )
                    }
                }

            paymentMethodsResponse.addPaymentMethodsItem(paymentMethod)
        }

        return paymentMethodsResponse
    }
}
