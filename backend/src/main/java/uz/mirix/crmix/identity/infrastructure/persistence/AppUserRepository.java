package uz.mirix.crmix.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.mirix.crmix.identity.domain.UserRole;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {
    Optional<AppUserEntity> findByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);
    long countByTenantIdAndRoleAndActiveTrue(UUID tenantId, String role);

    default long countActiveOwners(UUID tenantId) {
        return countByTenantIdAndRoleAndActiveTrue(tenantId, UserRole.OWNER.name());
    }
}
