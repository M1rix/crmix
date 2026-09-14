package uz.mirix.crmix.messaging.api;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.messaging.application.TelegramLinkService;
import uz.mirix.crmix.messaging.domain.NotificationStatus;
import uz.mirix.crmix.messaging.infrastructure.persistence.NotificationLogEntity;
import uz.mirix.crmix.messaging.infrastructure.persistence.NotificationLogRepository;
import uz.mirix.crmix.platform.tenancy.RlsTenantScope;
import uz.mirix.crmix.platform.tenancy.TenantContext;

@RestController
@RequestMapping("/api/v1")
public class MessagingController {
    private final TelegramLinkService linkService;
    private final TenantContext tenantContext;
    private final RlsTenantScope rlsTenantScope;
    private final NotificationLogRepository logRepository;

    public MessagingController(TelegramLinkService linkService, TenantContext tenantContext, RlsTenantScope rlsTenantScope, NotificationLogRepository logRepository) {
        this.linkService = linkService;
        this.tenantContext = tenantContext;
        this.rlsTenantScope = rlsTenantScope;
        this.logRepository = logRepository;
    }

    @PostMapping("/clients/{clientId}/telegram-link")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','STAFF')")
    TelegramLinkService.LinkResult createTelegramLink(@PathVariable UUID clientId) {
        return linkService.create(clientId);
    }

    @GetMapping("/notifications")
    @Transactional(readOnly = true)
    List<NotificationView> notifications(@RequestParam UUID appointmentId) {
        var tenantId = tenantContext.requireTenantId();
        rlsTenantScope.apply(tenantId);
        return logRepository.findByTenantIdAndAppointmentIdOrderByScheduledForAsc(tenantId, appointmentId).stream()
                .map(NotificationView::from)
                .toList();
    }

    public record NotificationView(UUID id, String templateCode, NotificationStatus status, java.time.Instant scheduledFor, int attempts, java.time.Instant sentAt, String error) {
        static NotificationView from(NotificationLogEntity entity) {
            return new NotificationView(entity.getId(), entity.getTemplateCode(), entity.getStatus(), entity.getScheduledFor(), entity.getAttemptCount(), entity.getSentAt(), entity.getErrorMessage());
        }
    }
}
