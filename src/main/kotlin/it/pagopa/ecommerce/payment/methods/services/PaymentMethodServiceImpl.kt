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
import java.util.concurrent.CompletionStage
import org.slf4j.LoggerFactory

@ApplicationScoped
class PaymentMethodServiceImpl @Inject constructor(private val restClient: PaymentMethodsClient) :
    PaymentMethodService {

    private val log = LoggerFactory.getLogger(PaymentMethodServiceImpl::class.java)

    override fun searchPaymentMethods(
        amount: BigDecimal,
        xClientId: String,
        xRequestId: String,
    ): CompletionStage<PaymentMethodsResponse> {
        return restClient
            .searchPaymentMethods(buildRequest(amount, xClientId), xRequestId)
            .map { dto -> paymentMethodDtoToResponse(dto) }
            .subscribeAsCompletionStage()
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
        paymentRequestDto.allCCp = false // TODO this is mocked

        return paymentRequestDto
    }

    private fun paymentMethodDtoToResponse(
        paymentMethodsResponseDto: PaymentMethodsResponseDto
    ): PaymentMethodsResponse {
        val paymentMethodsResponse = PaymentMethodsResponse()

        paymentMethodsResponseDto.paymentMethods.forEach { it ->
            try {
                val paymentMethod = PaymentMethodResponse()

                paymentMethod.id = it.paymentMethodId
                paymentMethod.name = it.name["IT"]
                paymentMethod.description = it.description["IT"]
                paymentMethod.status = PaymentMethodStatus.valueOf(it.status.toString())
                paymentMethod.ranges = listOf(Range().max(it.feeRange.max).min(it.feeRange.min))
                paymentMethod.paymentTypeCode = it.group.toString()
                paymentMethod.asset = it.paymentMethodAsset
                paymentMethod.methodManagement =
                    PaymentMethodManagementType.valueOf(it.methodManagement.toString())
                paymentMethod.brandAssets = it.paymentMethodsBrandAssets

                paymentMethodsResponse.addPaymentMethodsItem(paymentMethod)
            } catch (ex: Exception) {
                log.error("Failed to map payment method ${it.paymentMethodId}", ex)
            }
        }

        return paymentMethodsResponse
    }
}
