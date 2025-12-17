package it.pagopa.ecommerce.payment.methods.mappers

import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsItemDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentNoticeItemOptionalTransferListDto
import it.pagopa.generated.ecommerce.client.model.TransferListItemDto
import java.util.Optional

fun PaymentMethodsItemDto.toPaymentMethodResponse(): PaymentMethodResponse {
    val paymentHandlerPaymentMethod = PaymentMethodResponse()

    paymentHandlerPaymentMethod.id = this.paymentMethodId
    paymentHandlerPaymentMethod.name = this.name
    paymentHandlerPaymentMethod.description = this.description
    paymentHandlerPaymentMethod.status =
        PaymentMethodResponse.StatusEnum.valueOf(this.status.toString())
    paymentHandlerPaymentMethod.feeRange = null
    /**
     * CHK-4632 feeRange conversion disabled as the field is currently unused. Keep for potential
     * future re-enablement when AFM provides fee ranges. this.feeRange?.let { afmResponseFeeRange
     * -> FeeRange().max(afmResponseFeeRange.max).min(afmResponseFeeRange.min) }
     */
    paymentHandlerPaymentMethod.paymentTypeCode =
        PaymentMethodResponse.PaymentTypeCodeEnum.valueOf(this.group.toString())
    paymentHandlerPaymentMethod.paymentMethodAsset = this.paymentMethodAsset
    paymentHandlerPaymentMethod.methodManagement =
        PaymentMethodResponse.MethodManagementEnum.valueOf(this.methodManagement.toString())
    paymentHandlerPaymentMethod.paymentMethodsBrandAssets = this.paymentMethodsBrandAssets
    paymentHandlerPaymentMethod.validityDateFrom = this.validityDateFrom
    paymentHandlerPaymentMethod.disabledReason =
        this.disabledReason?.let {
            PaymentMethodResponse.DisabledReasonEnum.valueOf(this.disabledReason.toString())
        }
    paymentHandlerPaymentMethod.metadata = this.metadata
    paymentHandlerPaymentMethod.paymentMethodTypes =
        this.paymentMethodTypes?.let {
            this.paymentMethodTypes.map { payMethodType ->
                PaymentMethodResponse.PaymentMethodTypesEnum.valueOf(payMethodType.toString())
            }
        }

    return paymentHandlerPaymentMethod
}

fun PaymentMethodResponseDto.toPaymentMethodResponse(): PaymentMethodResponse {
    val paymentHandlerPaymentMethod = PaymentMethodResponse()

    paymentHandlerPaymentMethod.id = this.paymentMethodId
    paymentHandlerPaymentMethod.name = this.name
    paymentHandlerPaymentMethod.description = this.description
    paymentHandlerPaymentMethod.status =
        PaymentMethodResponse.StatusEnum.valueOf(this.status.toString())
    paymentHandlerPaymentMethod.feeRange = null
    /**
     * CHK-4632 feeRange conversion disabled as the field is currently unused. Keep for potential
     * future re-enablement when AFM provides fee ranges. this.rangeAmount?.let {
     * afmResponseFeeRange -> FeeRange().max(afmResponseFeeRange.max).min(afmResponseFeeRange.min) }
     */
    paymentHandlerPaymentMethod.paymentTypeCode =
        PaymentMethodResponse.PaymentTypeCodeEnum.valueOf(this.group.toString())
    paymentHandlerPaymentMethod.paymentMethodAsset = this.paymentMethodAsset
    paymentHandlerPaymentMethod.methodManagement =
        PaymentMethodResponse.MethodManagementEnum.valueOf(this.methodManagement.toString())
    paymentHandlerPaymentMethod.paymentMethodsBrandAssets = this.paymentMethodsBrandAssets
    paymentHandlerPaymentMethod.validityDateFrom = this.validityDateFrom
    paymentHandlerPaymentMethod.metadata = this.metadata
    paymentHandlerPaymentMethod.paymentMethodTypes =
        this.paymentMethodTypes?.let {
            this.paymentMethodTypes.map { payMethodType ->
                PaymentMethodResponse.PaymentMethodTypesEnum.valueOf(payMethodType.toString())
            }
        }

    return paymentHandlerPaymentMethod
}

fun PaymentMethodsResponseDto.toPaymentMethodsResponse(): PaymentMethodsResponse {
    val paymentHandlerResponse = PaymentMethodsResponse()

    this.paymentMethods.forEach { afmResponsePaymentMethod ->
        paymentHandlerResponse.addPaymentMethodsItem(
            afmResponsePaymentMethod.toPaymentMethodResponse()
        )
    }

    return paymentHandlerResponse
}

fun PaymentMethodsRequest.toPaymentMethodRequestDto(): PaymentMethodRequestDto {
    val afmRequest = PaymentMethodRequestDto()
    afmRequest.userTouchpoint =
        PaymentMethodRequestDto.UserTouchpointEnum.valueOf(this.userTouchpoint.toString())
    afmRequest.userDevice =
        this.userDevice?.let { device ->
            PaymentMethodRequestDto.UserDeviceEnum.valueOf(device.toString())
        }
    afmRequest.totalAmount = this.totalAmount
    this.paymentNotice.forEach { notice ->
        val paymentNotice = PaymentNoticeItemOptionalTransferListDto()
        paymentNotice.paymentAmount = notice.paymentAmount
        paymentNotice.primaryCreditorInstitution = notice.primaryCreditorInstitution
        notice.transferList?.forEach { transfer ->
            val transferListItem = TransferListItemDto()
            transferListItem.creditorInstitution = transfer.creditorInstitution
            transferListItem.transferCategory = transfer.transferCategory
            transferListItem.digitalStamp = transfer.digitalStamp

            paymentNotice.addTransferListItem(transferListItem)
        }

        afmRequest.paymentNotice.add(paymentNotice)
    }
    afmRequest.allCCp = this.allCCp
    afmRequest.targetKey = this.targetKey
    if (this.sortBy != null) {
        afmRequest.sortBy = PaymentMethodRequestDto.SortByEnum.valueOf(this.sortBy.value())
    }
    if (this.sortOrder != null) {
        afmRequest.sortOrder = PaymentMethodRequestDto.SortOrderEnum.valueOf(this.sortOrder.value())
    }
    if (this.language != null) {
        afmRequest.language = PaymentMethodRequestDto.LanguageEnum.valueOf(this.language.value())
    }
    afmRequest.priorityGroups =
        Optional.ofNullable(this.priorityGroups)
            .orElse(emptyList<PaymentMethodsRequest.PriorityGroupsEnum>())
            .stream()
            .map { g -> PaymentMethodRequestDto.PriorityGroupsEnum.valueOf(g.value()) }
            .toList()

    return afmRequest
}
