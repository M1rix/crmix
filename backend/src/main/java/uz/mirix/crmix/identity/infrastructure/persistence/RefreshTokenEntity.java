package uz.mirix.crmix.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshTokenEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUserEntity user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshTokenEntity() {}

    public static RefreshTokenEntity create(UUID tenantId, AppUserEntity user, String tokenHash, Instant expiresAt, Instant now) {
        var token = new RefreshTokenEntity();
        token.id = UUID.randomUUID();
        token.tenantId = tenantId;
        token.user = user;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        token.createdAt = now;
        return token;
    }

    public AppUserEntity getUser() { return user; }
    public UUID getTenantId() { return tenantId; }
    public boolean isUsableAt(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
    public void revoke(Instant now) { this.revokedAt = now; }
}
