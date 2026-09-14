package uz.mirix.crmix.directory.infrastructure.persistence;

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

@Entity
@Table(name = "employee")
public class EmployeeEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String specialization;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "work_schedule", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> workSchedule = new LinkedHashMap<>();

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected EmployeeEntity() {}

    public static EmployeeEntity create(UUID tenantId, String fullName, String specialization, Map<String, Object> workSchedule, Instant now) {
        var entity = new EmployeeEntity();
        entity.id = UUID.randomUUID();
        entity.tenantId = tenantId;
        entity.fullName = fullName;
        entity.specialization = specialization;
        entity.workSchedule = workSchedule == null ? new LinkedHashMap<>() : new LinkedHashMap<>(workSchedule);
        entity.active = true;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void update(String fullName, String specialization, Map<String, Object> workSchedule, boolean active, Instant now) {
        this.fullName = fullName;
        this.specialization = specialization;
        this.workSchedule = workSchedule == null ? new LinkedHashMap<>() : new LinkedHashMap<>(workSchedule);
        this.active = active;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getFullName() { return fullName; }
    public String getSpecialization() { return specialization; }
    public Map<String, Object> getWorkSchedule() { return workSchedule; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
