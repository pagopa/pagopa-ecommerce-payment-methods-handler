package it.pagopa.ecommerce.payment.methods.client

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodNotFoundException
import it.pagopa.ecommerce.payment.methods.exception.PaymentMethodsClientException
import it.pagopa.generated.ecommerce.client.api.PaymentMethodsApi
import it.pagopa.generated.ecommerce.client.model.PaymentMethodDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.ClientWebApplicationException
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
                PaymentMethodsClientException(
                    "Error during the call to PaymentMethodsApi.searchPaymentMethods",
                    error,
                )
            }
    }

    fun getPaymentMethod(paymentMethodsId: String, xRequestId: String): Uni<PaymentMethodDto> {
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
    }
}
