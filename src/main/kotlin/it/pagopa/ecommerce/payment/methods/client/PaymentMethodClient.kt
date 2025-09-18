package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class PaymentMethodsClient(@param:RestClient private val paymentMethodsApi: PaymentMethodsApi) {

    private val log = LoggerFactory.getLogger(PaymentMethodsClient::class.java)

    fun searchPaymentMethods(
        requestDto: PaymentMethodRequestDto,
        xRequestId: String,
    ): Uni<PaymentMethodsResponseDto> {

        return paymentMethodsApi
            .searchPaymentMethods(requestDto, xRequestId)
            .onFailure()
            .transform { error ->
                PaymentMethodsClientException("Error during the call to PaymentMethodsApi", error)
            }
    }
}
