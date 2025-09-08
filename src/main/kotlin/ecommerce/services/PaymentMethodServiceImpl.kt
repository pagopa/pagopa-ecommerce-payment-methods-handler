package ecommerce.services

import ecommerce.client.PaymentMethodsClient
import io.smallrye.mutiny.Uni
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class PaymentMethodServiceImpl @Inject constructor(private val restClient: PaymentMethodsClient) :
    PaymentMethodService {
    override fun searchPaymentMethods(
        paymentMethodRequestDto: PaymentMethodRequestDto,
        xRequestId: String,
    ): Uni<PaymentMethodsResponseDto> {
        return restClient.searchPaymentMethods(paymentMethodRequestDto, xRequestId)
    }
}
