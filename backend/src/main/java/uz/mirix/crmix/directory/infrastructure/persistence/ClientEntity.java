package uz.mirix.crmix.directory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client")
public class ClientEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ClientEntity() {}

    public static ClientEntity create(UUID tenantId, String fullName, String phone, String notes, Instant now) {
        var entity = new ClientEntity();
        entity.id = UUID.randomUUID();
        entity.tenantId = tenantId;
        entity.fullName = fullName;
        entity.phone = phone;
        entity.notes = notes;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void update(String fullName, String phone, String notes, Instant now) {
        this.fullName = fullName;
        this.phone = phone;
        this.notes = notes;
        this.updatedAt = now;
    }

    public void linkTelegram(long chatId, Instant now) {
        this.telegramChatId = chatId;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public Long getTelegramChatId() { return telegramChatId; }
    public String getNotes() { return notes; }
    public long getVersion() { return version; }
}
