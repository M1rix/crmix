package uz.mirix.crmix.messaging.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.mirix.crmix.messaging.application.TelegramLinkService;
import uz.mirix.crmix.messaging.infrastructure.telegram.TelegramGateway;

@RestController
@RequestMapping("/api/v1/integrations/telegram")
public class TelegramWebhookController {
    private final TelegramLinkService linkService;
    private final TelegramGateway gateway;
    private final String webhookSecret;

    public TelegramWebhookController(
            TelegramLinkService linkService,
            TelegramGateway gateway,
            @Value("${telegram.webhook-secret:${TELEGRAM_WEBHOOK_SECRET:}}") String webhookSecret) {
        this.linkService = linkService;
        this.gateway = gateway;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    void webhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String providedSecret,
            @RequestBody Update update) {
        if (!secureEquals(webhookSecret, providedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        var text = update.getMessage().getText().trim();
        var chatId = update.getMessage().getChatId();
        if (text.startsWith("/start ")) {
            linkService.consume(text.substring(7).trim(), chatId);
            gateway.send(chatId, "CRMIX: Telegram успешно подключён / muvaffaqiyatli ulandi.");
        }
    }

    private static boolean secureEquals(String expected, String actual) {
        if (expected == null || expected.isBlank() || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
