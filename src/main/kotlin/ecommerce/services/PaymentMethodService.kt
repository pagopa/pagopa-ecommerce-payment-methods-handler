package ecommerce.services

import io.smallrye.mutiny.Uni
import it.pagopa.generated.ecommerce.client.model.PaymentMethodRequestDto
import it.pagopa.generated.ecommerce.client.model.PaymentMethodsResponseDto

interface PaymentMethodService {
    fun searchPaymentMethods(
        paymentMethodRequestDto: PaymentMethodRequestDto,
        xRequestId: String,
    ): Uni<PaymentMethodsResponseDto>
}
