package it.pagopa.ecommerce.payment.methods.services

import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodManagementType
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodStatus
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.Range
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import it.pagopa.generated.ecommerce.client.model.PaymentNoticeItemDto
import it.pagopa.generated.ecommerce.client.model.TransferListItemDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture

@ApplicationScoped
class PaymentMethodServiceImpl @Inject constructor(private val restClient: PaymentMethodsClient) :
    PaymentMethodService {
    override fun searchPaymentMethods(
        amount: BigDecimal,
        xClientId: String,
        xRequestId: String,
    ): CompletableFuture<PaymentMethodsResponse> {
        return restClient
            .searchPaymentMethods(buildRequest(amount, xClientId), xRequestId)
            .map { dto -> paymentMethodDtoToResponse(dto) }
            .subscribeAsCompletionStage()
            .toCompletableFuture()
    }

    private fun buildRequest(amount: BigDecimal, xClientId: String): PaymentMethodRequestDto {
        val paymentRequestDto = PaymentMethodRequestDto()
        paymentRequestDto.userTouchpoint =
            PaymentMethodRequestDto.UserTouchpointEnum.valueOf(xClientId)
        paymentRequestDto.userDevice =
            PaymentMethodRequestDto.UserDeviceEnum.valueOf("WEB") // TODO this is mocked
        paymentRequestDto.totalAmount =
            amount.intValueExact() // TODO this should be fixed by GMP and set to Long/BigDecimal
        val paymentNotice = PaymentNoticeItemDto()
        paymentNotice.paymentAmount = amount.longValueExact()
        paymentNotice.primaryCreditorInstitution = "77777777777" // TODO this is mocked
        val transferListItem = TransferListItemDto()
        transferListItem.creditorInstitution = "77777777777" // TODO this is mocked
        paymentNotice.transferList = listOf(transferListItem)
        paymentRequestDto.paymentNotice = listOf(paymentNotice)

        return paymentRequestDto
    }

    private fun paymentMethodDtoToResponse(
        paymentMethodsResponseDto: PaymentMethodsResponseDto
    ): PaymentMethodsResponse {
        val paymentMethodsResponse = PaymentMethodsResponse()

        paymentMethodsResponseDto.paymentMethods.forEach { it ->
            val paymentMethod = PaymentMethodResponse()

            paymentMethod.id = it.paymentMethodId
            paymentMethod.name = it.name["it"]
            paymentMethod.description = it.description["it"]
            paymentMethod.status = PaymentMethodStatus.valueOf(it.status.toString())
            paymentMethod.ranges = listOf(Range().max(it.feeRange.max).min(it.feeRange.min))
            paymentMethod.paymentTypeCode = it.group.toString()
            paymentMethod.asset = it.paymentMethodAsset
            paymentMethod.methodManagement =
                PaymentMethodManagementType.valueOf(it.methodManagement.toString())
            paymentMethod.brandAssets = it.paymentMethodsBrandAssets

            paymentMethodsResponse.addPaymentMethodsItem(paymentMethod)
        }

        return paymentMethodsResponse
    }
}
