package it.pagopa.ecommerce.payment.methods.utils

import it.pagopa.generated.ecommerce.client.model.BundleOptionDto

object BundleOptions {

    @JvmStatic
    fun removeDuplicatePsp(optionDto: BundleOptionDto): BundleOptionDto {
        optionDto.bundleOptions = optionDto.bundleOptions?.distinctBy { it.idPsp } ?: emptyList()
        return optionDto
    }
}
