package uz.mirix.crmix.messaging.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.directory.infrastructure.persistence.ClientRepository;
import uz.mirix.crmix.messaging.infrastructure.persistence.TelegramLinkTokenEntity;
import uz.mirix.crmix.messaging.infrastructure.persistence.TelegramLinkTokenRepository;
import uz.mirix.crmix.platform.error.ApiException;
import uz.mirix.crmix.platform.tenancy.RlsTenantScope;
import uz.mirix.crmix.platform.tenancy.TenantContext;

@Service
public class TelegramLinkService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final TenantContext tenantContext;
    private final RlsTenantScope rlsTenantScope;
    private final ClientRepository clientRepository;
    private final TelegramLinkTokenRepository tokenRepository;
    private final String botUsername;

    public TelegramLinkService(
            TenantContext tenantContext,
            RlsTenantScope rlsTenantScope,
            ClientRepository clientRepository,
            TelegramLinkTokenRepository tokenRepository,
            @Value("${telegram.bot-username:${TELEGRAM_BOT_USERNAME:}}") String botUsername) {
        this.tenantContext = tenantContext;
        this.rlsTenantScope = rlsTenantScope;
        this.clientRepository = clientRepository;
        this.tokenRepository = tokenRepository;
        this.botUsername = botUsername;
    }

    @Transactional
    public LinkResult create(UUID clientId) {
        var tenantId = tenantContext.requireTenantId();
        rlsTenantScope.apply(tenantId);
        clientRepository.findByIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "client-not-found", "Not found", "Client does not exist"));
        if (botUsername == null || botUsername.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "telegram-not-configured", "Telegram is not configured", "TELEGRAM_BOT_USERNAME is missing");
        }
        var raw = tenantId + "." + randomPart();
        tokenRepository.save(TelegramLinkTokenEntity.create(tenantId, clientId, hash(raw), Instant.now()));
        return new LinkResult("https://t.me/" + botUsername.replace("@", "") + "?start=" + raw, 900);
    }

    @Transactional
    public UUID consume(String rawToken, long chatId) {
        var tenantId = tenantFrom(rawToken);
        rlsTenantScope.apply(tenantId);
        var token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> invalidToken());
        var now = Instant.now();
        if (!token.getTenantId().equals(tenantId) || !token.usableAt(now)) {
            throw invalidToken();
        }
        var client = clientRepository.findByIdAndTenantId(token.getClientId(), tenantId)
                .orElseThrow(() -> invalidToken());
        client.linkTelegram(chatId, now);
        token.use(now);
        return client.getId();
    }

    private static UUID tenantFrom(String token) {
        try {
            var separator = token.indexOf('.');
            return UUID.fromString(token.substring(0, separator));
        } catch (RuntimeException exception) {
            throw invalidToken();
        }
    }

    private static String randomPart() {
        var bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid-telegram-link", "Invalid Telegram link", "The link is invalid, expired or already used");
    }

    public record LinkResult(String url, int expiresInSeconds) {}
}
