package uz.mirix.crmix.messaging.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.mirix.crmix.messaging.domain.NotificationStatus;

@Entity
@Table(name = "notification_log")
public class NotificationLogEntity {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "client_id", nullable = false) private UUID clientId;
    @Column(name = "appointment_id") private UUID appointmentId;
    @Column(nullable = false) private String channel;
    @Column(name = "template_code", nullable = false) private String templateCode;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private Map<String, Object> payload = new LinkedHashMap<>();
    @Column(nullable = false) private String status;
    @Column(name = "scheduled_for", nullable = false) private Instant scheduledFor;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "error_message") private String errorMessage;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected NotificationLogEntity() {}

    public static NotificationLogEntity pending(UUID tenantId, UUID clientId, UUID appointmentId, String templateCode, Map<String, Object> payload, Instant scheduledFor, Instant now) {
        var entity = new NotificationLogEntity();
        entity.id = UUID.randomUUID();
        entity.tenantId = tenantId;
        entity.clientId = clientId;
        entity.appointmentId = appointmentId;
        entity.channel = "TELEGRAM";
        entity.templateCode = templateCode;
        entity.payload = new LinkedHashMap<>(payload);
        entity.status = NotificationStatus.PENDING.name();
        entity.scheduledFor = scheduledFor;
        entity.nextAttemptAt = scheduledFor.isAfter(now) ? scheduledFor : now;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void markSent(Instant now) {
        attemptCount++;
        status = NotificationStatus.SENT.name();
        sentAt = now;
        errorMessage = null;
        updatedAt = now;
    }

    public void markFailed(String error, Instant now) {
        attemptCount++;
        errorMessage = error == null ? "Telegram delivery failed" : error.substring(0, Math.min(2000, error.length()));
        updatedAt = now;
        if (attemptCount >= 3) {
            status = NotificationStatus.FAILED.name();
            return;
        }
        status = NotificationStatus.RETRY.name();
        var backoffSeconds = attemptCount == 1 ? 60L : 300L;
        nextAttemptAt = now.plusSeconds(backoffSeconds);
    }

    public void cancel(Instant now) {
        if (status.equals(NotificationStatus.PENDING.name()) || status.equals(NotificationStatus.RETRY.name())) {
            status = NotificationStatus.CANCELLED.name();
            updatedAt = now;
        }
    }

    public void skip(String reason, Instant now) {
        status = NotificationStatus.SKIPPED.name();
        errorMessage = reason;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getClientId() { return clientId; }
    public UUID getAppointmentId() { return appointmentId; }
    public String getTemplateCode() { return templateCode; }
    public Map<String, Object> getPayload() { return payload; }
    public NotificationStatus getStatus() { return NotificationStatus.valueOf(status); }
    public Instant getScheduledFor() { return scheduledFor; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getSentAt() { return sentAt; }
    public String getErrorMessage() { return errorMessage; }
}
