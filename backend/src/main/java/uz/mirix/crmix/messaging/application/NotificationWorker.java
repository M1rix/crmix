package uz.mirix.crmix.messaging.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.directory.infrastructure.persistence.ClientRepository;
import uz.mirix.crmix.messaging.infrastructure.persistence.NotificationLogEntity;
import uz.mirix.crmix.messaging.infrastructure.persistence.NotificationLogRepository;
import uz.mirix.crmix.messaging.infrastructure.persistence.NotificationTemplateRepository;
import uz.mirix.crmix.messaging.infrastructure.telegram.TelegramGateway;
import uz.mirix.crmix.platform.tenancy.RlsTenantScope;

@Service
public class NotificationWorker {
    private final RlsTenantScope rlsTenantScope;
    private final NotificationLogRepository logRepository;
    private final NotificationTemplateRepository templateRepository;
    private final ClientRepository clientRepository;
    private final TelegramGateway telegramGateway;

    public NotificationWorker(
            RlsTenantScope rlsTenantScope,
            NotificationLogRepository logRepository,
            NotificationTemplateRepository templateRepository,
            ClientRepository clientRepository,
            TelegramGateway telegramGateway) {
        this.rlsTenantScope = rlsTenantScope;
        this.logRepository = logRepository;
        this.templateRepository = templateRepository;
        this.clientRepository = clientRepository;
        this.telegramGateway = telegramGateway;
    }

    @Transactional
    public int processTenant(UUID tenantId) {
        rlsTenantScope.apply(tenantId);
        var now = Instant.now();
        var due = logRepository.findByTenantIdAndStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                tenantId, List.of("PENDING", "RETRY"), now, PageRequest.of(0, 50));
        due.forEach(notification -> deliver(tenantId, notification, now));
        return due.size();
    }

    private void deliver(UUID tenantId, NotificationLogEntity notification, Instant now) {
        var client = clientRepository.findByIdAndTenantId(notification.getClientId(), tenantId).orElse(null);
        if (client == null || client.getTelegramChatId() == null) {
            notification.skip("Client has no Telegram chat linked", now);
            return;
        }
        var locale = client.getLocale() == null ? "ru" : client.getLocale();
        var template = templateRepository.findByTenantIdAndCodeAndLocale(tenantId, notification.getTemplateCode(), locale)
                .or(() -> templateRepository.findByTenantIdIsNullAndCodeAndLocale(notification.getTemplateCode(), locale))
                .or(() -> templateRepository.findByTenantIdIsNullAndCodeAndLocale(notification.getTemplateCode(), "ru"))
                .orElseThrow(() -> new IllegalStateException("Notification template is missing: " + notification.getTemplateCode()));
        var text = render(template.getBody(), notification.getPayload());
        try {
            telegramGateway.send(client.getTelegramChatId(), text);
            notification.markSent(now);
        } catch (RuntimeException exception) {
            notification.markFailed(exception.getMessage(), now);
        }
    }

    private static String render(String template, java.util.Map<String, Object> payload) {
        var result = template;
        for (var entry : payload.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
