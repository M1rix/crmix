package uz.mirix.crmix.platform.tenancy;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RlsTenantScope {
    private final EntityManager entityManager;

    public RlsTenantScope(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void apply(UUID tenantId) {
        entityManager
                .createNativeQuery("select set_config('app.tenant_id', :tenantId, true)")
                .setParameter("tenantId", tenantId.toString())
                .getSingleResult();
    }

    public void apply(TenantContext tenantContext) {
        apply(tenantContext.requireTenantId());
    }
}
