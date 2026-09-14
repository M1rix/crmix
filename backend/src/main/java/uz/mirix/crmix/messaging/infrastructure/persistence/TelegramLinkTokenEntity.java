package uz.mirix.crmix.messaging.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telegram_link_token")
public class TelegramLinkTokenEntity {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "client_id", nullable = false) private UUID clientId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected TelegramLinkTokenEntity() {}

    public static TelegramLinkTokenEntity create(UUID tenantId, UUID clientId, String tokenHash, Instant now) {
        var entity = new TelegramLinkTokenEntity();
        entity.id = UUID.randomUUID();
        entity.tenantId = tenantId;
        entity.clientId = clientId;
        entity.tokenHash = tokenHash;
        entity.expiresAt = now.plusSeconds(15 * 60L);
        entity.createdAt = now;
        return entity;
    }

    public UUID getTenantId() { return tenantId; }
    public UUID getClientId() { return clientId; }
    public boolean usableAt(Instant now) { return usedAt == null && expiresAt.isAfter(now); }
    public void use(Instant now) { usedAt = now; }
}
