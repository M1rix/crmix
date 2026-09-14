package uz.mirix.crmix.messaging.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, UUID> {
    List<NotificationLogEntity> findByTenantIdAndStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            UUID tenantId, Collection<String> statuses, Instant now, Pageable pageable);
    List<NotificationLogEntity> findByTenantIdAndAppointmentIdOrderByScheduledForAsc(UUID tenantId, UUID appointmentId);
    List<NotificationLogEntity> findTop100ByTenantIdOrderByScheduledForDesc(UUID tenantId);
}
