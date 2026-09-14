package uz.mirix.crmix.billing.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.mirix.crmix.billing.application.PaymeMerchantService;
import uz.mirix.crmix.billing.application.PaymeRpcException;

@RestController
@RequestMapping("/api/v1/webhooks/payme")
public class PaymeWebhookController {
    private final PaymeMerchantService payme;
    private final String expectedAuthorization;

    public PaymeWebhookController(
            PaymeMerchantService payme,
            @Value("${payme.login:${PAYME_LOGIN:}}") String login,
            @Value("${payme.secret:${PAYME_SECRET:}}") String secret) {
        this.payme = payme;
        this.expectedAuthorization = "Basic " + Base64.getEncoder().encodeToString((login + ":" + secret).getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping
    Map<String, Object> webhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody JsonNode request) {
        var id = request.get("id");
        if (!secureEquals(expectedAuthorization, authorization)) return error(id, -32504, "Insufficient privileges", null);
        var method = request.path("method").asText();
        if (method.isBlank() || !request.has("params")) return error(id, -32600, "Invalid request", null);
        try {
            return Map.of("id", id == null ? 0 : id, "result", payme.handle(method, request.path("params")));
        } catch (PaymeRpcException exception) {
            return error(id, exception.code(), exception.getMessage(), exception.data());
        } catch (RuntimeException exception) {
            return error(id, -32400, "Internal error", null);
        }
    }

    private static Map<String, Object> error(JsonNode id, int code, String message, String data) {
        var error = new LinkedHashMap<String, Object>();
        error.put("code", code);
        error.put("message", message);
        if (data != null) error.put("data", data);
        return Map.of("id", id == null ? 0 : id, "error", error);
    }

    private static boolean secureEquals(String expected, String actual) {
        if (actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
