package uz.mirix.crmix.messaging.application;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.messaging.infrastructure.persistence.NotificationLogEntity;
import uz.mirix.crmix.messaging.infrastructure.persistence.NotificationLogRepository;
import uz.mirix.crmix.platform.tenancy.RlsTenantScope;
import uz.mirix.crmix.scheduling.application.AppointmentCancelledEvent;
import uz.mirix.crmix.scheduling.application.AppointmentCreatedEvent;

@Service
public class ReminderPlanner {
    private final RlsTenantScope rlsTenantScope;
    private final NotificationLogRepository repository;

    public ReminderPlanner(RlsTenantScope rlsTenantScope, NotificationLogRepository repository) {
        this.rlsTenantScope = rlsTenantScope;
        this.repository = repository;
    }

    @Transactional
    public void plan(AppointmentCreatedEvent event) {
        rlsTenantScope.apply(event.tenantId());
        var now = Instant.now();
        var localTime = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.of(event.timeZone()))
                .format(event.scheduledAt());
        var payload = Map.<String, Object>of(
                "time", localTime,
                "employee", event.employeeName(),
                "service", event.serviceName());
        repository.save(NotificationLogEntity.pending(
                event.tenantId(), event.clientId(), event.appointmentId(), "APPOINTMENT_REMINDER_24H", payload, event.scheduledAt().minusSeconds(24 * 3600L), now));
        repository.save(NotificationLogEntity.pending(
                event.tenantId(), event.clientId(), event.appointmentId(), "APPOINTMENT_REMINDER_2H", payload, event.scheduledAt().minusSeconds(2 * 3600L), now));
    }

    @Transactional
    public void cancel(AppointmentCancelledEvent event) {
        rlsTenantScope.apply(event.tenantId());
        var now = Instant.now();
        repository.findByTenantIdAndAppointmentIdOrderByScheduledForAsc(event.tenantId(), event.appointmentId())
                .forEach(notification -> notification.cancel(now));
    }
}
