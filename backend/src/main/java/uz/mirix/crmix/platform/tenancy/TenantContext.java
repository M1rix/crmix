package uz.mirix.crmix.platform.tenancy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class TenantContext {
    private UUID tenantId;

    public UUID requireTenantId() {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not available for this request");
        }
        return tenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        if (this.tenantId != null && !this.tenantId.equals(tenantId)) {
            throw new IllegalStateException("Tenant context cannot be changed during a request");
        }
        this.tenantId = tenantId;
    }
}
