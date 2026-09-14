package uz.mirix.crmix.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.mirix.crmix.identity.domain.SubscriptionStatus;
import uz.mirix.crmix.identity.infrastructure.persistence.TenantRepository;
import uz.mirix.crmix.platform.tenancy.TenantContext;

@Component
public class SubscriptionAccessFilter extends OncePerRequestFilter {
    private final TenantContext tenantContext;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    public SubscriptionAccessFilter(TenantContext tenantContext, TenantRepository tenantRepository, ObjectMapper objectMapper) {
        this.tenantContext = tenantContext;
        this.tenantRepository = tenantRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getRequestURI();
        return path.startsWith("/api/v1/billing") || path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/webhooks") || path.startsWith("/api/v1/integrations") || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var tenantId = tenantContext.getTenantId();
        if (tenantId != null) {
            var tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant != null && tenant.getSubscriptionStatus() == SubscriptionStatus.SUSPENDED) {
                response.setStatus(402);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), Map.of(
                        "type", "urn:crmix:problem:subscription-suspended",
                        "title", "Subscription suspended",
                        "status", 402,
                        "detail", "Renew the subscription to continue using CRMIX"));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static final class Map {
        private Map() {}
        static java.util.Map<String, Object> of(Object... values) {
            var result = new java.util.LinkedHashMap<String, Object>();
            for (int i = 0; i < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
            return result;
        }
    }
}
