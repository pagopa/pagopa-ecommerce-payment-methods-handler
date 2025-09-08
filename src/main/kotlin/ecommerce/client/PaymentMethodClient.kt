package ecommerce.client

import ecommerce.exception.PaymentMethodsClientException
import io.smallrye.mutiny.Uni
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class PaymentMethodsClient(@RestClient private val paymentMethodsApi: PaymentMethodsApi) {

    private val log = LoggerFactory.getLogger(PaymentMethodsClient::class.java)

    fun searchPaymentMethods(
        requestDto: PaymentMethodRequestDto,
        xRequestId: String,
    ): Uni<PaymentMethodsResponseDto> {
        return try {
            paymentMethodsApi
                .searchPaymentMethods(requestDto, xRequestId)
                .onItem()
                .transform { response -> response }
                .onFailure()
                .invoke { error ->
                    log.error("Error while calling PaymentMethodsApi.searchPaymentMethods", error)
                }
                .onFailure()
                .transform { error ->
                    PaymentMethodsClientException(
                        "Error during the call to PaymentMethodsApi",
                        error,
                    )
                }
        } catch (ex: Exception) {
            log.error("Unexpected error while calling PaymentMethodsApi", ex)
            Uni.createFrom()
                .failure(
                    PaymentMethodsClientException(
                        "Unexpected error while calling PaymentMethodsApi",
                        ex,
                    )
                )
        }
    }
}
