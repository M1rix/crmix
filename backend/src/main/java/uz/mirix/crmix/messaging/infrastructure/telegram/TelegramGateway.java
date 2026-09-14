package uz.mirix.crmix.messaging.infrastructure.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class TelegramGateway {
    private final String botToken;

    public TelegramGateway(@Value("${telegram.bot-token:${TELEGRAM_BOT_TOKEN:}}") String botToken) {
        this.botToken = botToken;
    }

    public void send(long chatId, String text) {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("TELEGRAM_BOT_TOKEN is not configured");
        }
        try {
            var client = new OkHttpTelegramClient(botToken);
            client.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (Exception exception) {
            throw new IllegalStateException("Telegram delivery failed", exception);
        }
    }
}
