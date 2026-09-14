package uz.mirix.crmix.scheduling.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import uz.mirix.crmix.scheduling.domain.AppointmentStatus;

@Entity
@Table(name = "appointment")
public class AppointmentEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private String status;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected AppointmentEntity() {}

    public static AppointmentEntity create(UUID tenantId, UUID clientId, UUID employeeId, UUID serviceId, Instant scheduledAt, int durationMinutes, Instant now) {
        var entity = new AppointmentEntity();
        entity.id = UUID.randomUUID();
        entity.tenantId = tenantId;
        entity.clientId = clientId;
        entity.employeeId = employeeId;
        entity.serviceId = serviceId;
        entity.scheduledAt = scheduledAt;
        entity.durationMinutes = durationMinutes;
        entity.status = AppointmentStatus.SCHEDULED.name();
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void transition(AppointmentStatus target, String cancellationReason, Instant now) {
        this.status = target.name();
        this.cancellationReason = target == AppointmentStatus.CANCELLED ? cancellationReason : null;
        this.updatedAt = now;
    }

    public void reschedule(Instant scheduledAt, Instant now) {
        this.scheduledAt = scheduledAt;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getClientId() { return clientId; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getServiceId() { return serviceId; }
    public Instant getScheduledAt() { return scheduledAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public AppointmentStatus getStatus() { return AppointmentStatus.valueOf(status); }
    public String getCancellationReason() { return cancellationReason; }
    public long getVersion() { return version; }
}
