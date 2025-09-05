package ecommerce.dto

class PaymentMethod {
    var paymentMethodID: String? = null
    var paymentMethodName: String? = null
    var paymentMethodDescription: String? = null
    var paymentMethodStatus: String? = null
    var paymentMethodAsset: String? = null
    var paymentMethodRanges: List<Range?>? = null
    var paymentMethodTypeCode: String? = null
    var clientId: String? = null
    var methodManagement: String? = null
    var paymentMethodsBrandAssets: Map<String?, String?>? = null
}
