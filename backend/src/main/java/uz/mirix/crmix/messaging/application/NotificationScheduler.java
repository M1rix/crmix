package uz.mirix.crmix.messaging.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.mirix.crmix.identity.infrastructure.persistence.TenantRepository;

@Component
public class NotificationScheduler {
    private final TenantRepository tenantRepository;
    private final NotificationWorker notificationWorker;

    public NotificationScheduler(TenantRepository tenantRepository, NotificationWorker notificationWorker) {
        this.tenantRepository = tenantRepository;
        this.notificationWorker = notificationWorker;
    }

    @Scheduled(fixedDelayString = "${messaging.worker-delay-ms:60000}")
    public void deliverDueNotifications() {
        tenantRepository.findAll().forEach(tenant -> notificationWorker.processTenant(tenant.getId()));
    }
}
