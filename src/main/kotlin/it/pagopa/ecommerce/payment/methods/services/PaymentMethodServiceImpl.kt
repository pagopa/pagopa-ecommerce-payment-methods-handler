package it.pagopa.ecommerce.payment.methods.services

import it.pagopa.ecommerce.payment.methods.client.PaymentMethodsClient
import it.pagopa.ecommerce.payment.methods.mappers.toPaymentMethodRequestDto
import it.pagopa.ecommerce.payment.methods.mappers.toPaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.mappers.toPaymentMethodsResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodResponse
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsRequest
import it.pagopa.ecommerce.payment.methods.v1.server.model.PaymentMethodsResponse
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
            .searchPaymentMethods(paymentMethodsRequest.toPaymentMethodRequestDto(), xRequestId)
            .map { dto -> dto.toPaymentMethodsResponse() }
            .map { dto -> filterMethods(paymentMethodsRequest, dto) }
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

    fun filterMethods(
        paymentMethodRequest: PaymentMethodsRequest,
        paymentMethodsResponse: PaymentMethodsResponse,
    ): PaymentMethodsResponse {
        val deviceVersion = paymentMethodRequest.deviceVersion
        val userTouchpoint = paymentMethodRequest.userTouchpoint
        return when (userTouchpoint) {
            /*
            payment methods filtering logic:
            we should not return CARDS payment method with ONBOARDABLE for old app version that will not handle this payment method
            Actually we identify IO app using user touchpoint field
            and old app version based on the fact that deviceVersion parameter is not valued.
            For those versions method management for card method is overridden to ONBOARDABLE_ONLY
            */
            PaymentMethodsRequest.UserTouchpointEnum.IO ->
                if (deviceVersion == null) {
                    paymentMethodsResponse.paymentMethods(
                        paymentMethodsResponse.paymentMethods.map {
                            if (
                                it.paymentTypeCode == PaymentMethodResponse.PaymentTypeCodeEnum.CP
                            ) {
                                it.methodManagement =
                                    PaymentMethodResponse.MethodManagementEnum.ONBOARDABLE_ONLY
                            }
                            it
                        }
                    )
                } else {
                    paymentMethodsResponse
                }

            else -> paymentMethodsResponse
        }
    }

    override fun getPaymentMethod(
        paymentMethodsId: String,
        xRequestId: String,
        xClientId: String,
    ): CompletionStage<PaymentMethodResponse> {
        return restClient
            .getPaymentMethod(paymentMethodsId, xRequestId, xClientId)
            .map { dto -> dto.toPaymentMethodResponse() }
            .onFailure()
            .invoke { exception ->
                log.error(
                    "Exception during request with id $xRequestId and client id $xClientId",
                    exception,
                )
            }
            .onItem()
            .invoke { _ ->
                log.info(
                    "Payment method retrieved successfully for request with id $xRequestId and client id $xClientId"
                )
            }
            .subscribeAsCompletionStage()
    }
}
