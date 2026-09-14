package uz.mirix.crmix.directory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service")
public class ServiceEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ServiceEntity() {}

    public static ServiceEntity create(UUID tenantId, String name, int durationMinutes, BigDecimal price, Instant now) {
        var entity = new ServiceEntity();
        entity.id = UUID.randomUUID();
        entity.tenantId = tenantId;
        entity.name = name;
        entity.durationMinutes = durationMinutes;
        entity.price = price;
        entity.active = true;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void update(String name, int durationMinutes, BigDecimal price, boolean active, Instant now) {
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.active = active;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public int getDurationMinutes() { return durationMinutes; }
    public BigDecimal getPrice() { return price; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
