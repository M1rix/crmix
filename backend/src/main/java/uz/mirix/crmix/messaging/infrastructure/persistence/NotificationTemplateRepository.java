package uz.mirix.crmix.messaging.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, UUID> {
    Optional<NotificationTemplateEntity> findByTenantIdAndCodeAndLocale(UUID tenantId, String code, String locale);
    Optional<NotificationTemplateEntity> findByTenantIdIsNullAndCodeAndLocale(String code, String locale);
}
