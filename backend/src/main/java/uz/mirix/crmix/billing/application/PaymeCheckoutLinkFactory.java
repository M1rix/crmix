package uz.mirix.crmix.billing.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymeCheckoutLinkFactory {
    private final String checkoutUrl;
    private final String merchantId;
    private final String callbackUrl;

    public PaymeCheckoutLinkFactory(
            @Value("${billing.payme.checkout-url:https://test.paycom.uz}") String checkoutUrl,
            @Value("${billing.payme.merchant-id:}") String merchantId,
            @Value("${billing.payme.callback-url:http://localhost:5173/billing}") String callbackUrl) {
        this.checkoutUrl = stripTrailingSlash(checkoutUrl);
        this.merchantId = merchantId == null ? "" : merchantId.trim();
        this.callbackUrl = callbackUrl;
    }

    public String create(UUID tenantId, UUID paymentId, BigDecimal amount) {
        if (merchantId.isBlank()) return null;
        long tiyin = amount.movePointRight(2).longValueExact();
        var params = "m=" + merchantId
                + ";ac.tenant_id=" + tenantId
                + ";ac.payment_id=" + paymentId
                + ";a=" + tiyin
                + ";l=ru"
                + ";c=" + callbackUrl;
        var encoded = Base64.getEncoder().encodeToString(params.getBytes(StandardCharsets.UTF_8));
        return checkoutUrl + "/" + encoded;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://test.paycom.uz";
        var normalized = value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }
}
