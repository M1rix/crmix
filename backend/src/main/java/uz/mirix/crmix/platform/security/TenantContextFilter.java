package uz.mirix.crmix.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.mirix.crmix.platform.tenancy.TenantContext;

@Component
public class TenantContextFilter extends OncePerRequestFilter {
    private final TenantContext tenantContext;

    public TenantContextFilter(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            var tenantClaim = jwtAuthentication.getToken().getClaimAsString("tenant_id");
            if (tenantClaim != null && !tenantClaim.isBlank()) {
                tenantContext.setTenantId(UUID.fromString(tenantClaim));
            }
        }
        filterChain.doFilter(request, response);
    }
}
