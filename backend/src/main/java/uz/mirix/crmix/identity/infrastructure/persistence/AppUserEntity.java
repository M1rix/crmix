package uz.mirix.crmix.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import uz.mirix.crmix.identity.domain.UserRole;

@Entity
@Table(name = "app_user")
public class AppUserEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected AppUserEntity() {}

    public static AppUserEntity owner(UUID tenantId, String email, String passwordHash, String fullName, String phone, Instant now) {
        var user = new AppUserEntity();
        user.id = UUID.randomUUID();
        user.tenantId = tenantId;
        user.email = email.toLowerCase().trim();
        user.passwordHash = passwordHash;
        user.role = UserRole.OWNER.name();
        user.fullName = fullName;
        user.phone = phone;
        user.active = true;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return UserRole.valueOf(role); }
    public String getFullName() { return fullName; }
    public boolean isActive() { return active; }
}
