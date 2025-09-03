package it.pagopa.ecommerce.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PaymentMethod {

    private String paymentMethodID;
    private String paymentMethodName;
    private String paymentMethodDescription;
    private String paymentMethodStatus;
    private String paymentMethodAsset;
    private List<Range> paymentMethodRanges;
    private String paymentMethodTypeCode;
    private String clientId;
    private String methodManagement;
    private Map<String, String> paymentMethodsBrandAssets;
}
