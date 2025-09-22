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
