package uz.mirix.crmix.messaging.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uz.mirix.crmix.messaging.infrastructure.telegram.TelegramGateway;

@Service
public class SupportService {
    private final TelegramGateway gateway;
    private final Long supportChatId;

    public SupportService(
            TelegramGateway gateway,
            @Value("${telegram.support-chat-id:${SUPPORT_TELEGRAM_CHAT_ID:}}") String supportChatId) {
        this.gateway = gateway;
        this.supportChatId = supportChatId == null || supportChatId.isBlank() ? null : Long.valueOf(supportChatId.trim());
    }

    public void submit(long requesterChatId, String text) {
        var normalized = text == null ? "" : text.trim();
        if (normalized.isBlank()) {
            gateway.send(requesterChatId, "CRMIX support: отправьте сообщение после /support. Masalan: /support Не открывается календарь");
            return;
        }
        if (supportChatId == null) {
            gateway.send(requesterChatId, "CRMIX support пока не настроен. Администратор уже видит эту проблему в конфигурации сервиса.");
            return;
        }
        if (normalized.length() > 3000) normalized = normalized.substring(0, 3000);
        gateway.send(supportChatId, "CRMIX support request\nTelegram chat: " + requesterChatId + "\n\n" + normalized);
        gateway.send(requesterChatId, "Запрос в CRMIX support принят. Javob shu Telegram chat orqali yuboriladi.");
    }
}
