package uz.mirix.crmix.messaging.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "notification_template")
public class NotificationTemplateEntity {
    @Id private UUID id;
    @Column(name = "tenant_id") private UUID tenantId;
    @Column(nullable = false) private String code;
    @Column(nullable = false) private String locale;
    @Column(nullable = false) private String body;

    protected NotificationTemplateEntity() {}

    public String getBody() { return body; }
}
