package uz.mirix.crmix.directory.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.directory.infrastructure.persistence.ClientEntity;
import uz.mirix.crmix.directory.infrastructure.persistence.ClientRepository;
import uz.mirix.crmix.directory.infrastructure.persistence.EmployeeEntity;
import uz.mirix.crmix.directory.infrastructure.persistence.EmployeeRepository;
import uz.mirix.crmix.directory.infrastructure.persistence.ServiceEntity;
import uz.mirix.crmix.directory.infrastructure.persistence.ServiceRepository;
import uz.mirix.crmix.identity.infrastructure.persistence.TenantRepository;
import uz.mirix.crmix.platform.error.ApiException;
import uz.mirix.crmix.platform.tenancy.RlsTenantScope;
import uz.mirix.crmix.platform.tenancy.TenantContext;

@Service
public class DirectoryService {
    private final TenantContext tenantContext;
    private final RlsTenantScope rlsTenantScope;
    private final TenantRepository tenantRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;

    public DirectoryService(
            TenantContext tenantContext,
            RlsTenantScope rlsTenantScope,
            TenantRepository tenantRepository,
            ClientRepository clientRepository,
            EmployeeRepository employeeRepository,
            ServiceRepository serviceRepository) {
        this.tenantContext = tenantContext;
        this.rlsTenantScope = rlsTenantScope;
        this.tenantRepository = tenantRepository;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientView> clients(String query, int page, int size) {
        var tenantId = applyTenant();
        var pageable = PageRequest.of(page, boundedSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        var normalized = query == null || query.isBlank() ? null : query.trim();
        return PageResponse.from(clientRepository.search(tenantId, normalized, pageable), ClientView::from);
    }

    @Transactional
    public ClientView createClient(String fullName, String phone, String notes) {
        var tenantId = applyTenant();
        var normalizedPhone = normalizePhone(phone);
        if (clientRepository.existsByTenantIdAndPhone(tenantId, normalizedPhone)) {
            throw new ApiException(HttpStatus.CONFLICT, "client-phone-conflict", "Client already exists", "A client with this phone already exists");
        }
        return ClientView.from(clientRepository.save(ClientEntity.create(tenantId, fullName.trim(), normalizedPhone, blankToNull(notes), Instant.now())));
    }

    @Transactional
    public ClientView updateClient(UUID id, String fullName, String phone, String notes) {
        var tenantId = applyTenant();
        var entity = clientRepository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound("client"));
        var normalizedPhone = normalizePhone(phone);
        if (!entity.getPhone().equals(normalizedPhone) && clientRepository.existsByTenantIdAndPhone(tenantId, normalizedPhone)) {
            throw new ApiException(HttpStatus.CONFLICT, "client-phone-conflict", "Client already exists", "A client with this phone already exists");
        }
        entity.update(fullName.trim(), normalizedPhone, blankToNull(notes), Instant.now());
        return ClientView.from(entity);
    }

    @Transactional
    public void deleteClient(UUID id) {
        var tenantId = applyTenant();
        var entity = clientRepository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound("client"));
        clientRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeView> employees(int page, int size) {
        var tenantId = applyTenant();
        var pageable = PageRequest.of(page, boundedSize(size), Sort.by("fullName"));
        return PageResponse.from(employeeRepository.findAllByTenantId(tenantId, pageable), EmployeeView::from);
    }

    @Transactional
    public EmployeeView createEmployee(String fullName, String specialization, Map<String, Object> workSchedule) {
        var tenantId = applyTenant();
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> notFound("tenant"));
        var activeCount = employeeRepository.countByTenantIdAndActiveTrue(tenantId);
        if (activeCount >= tenant.getSubscriptionPlan().getMaxEmployees()) {
            throw new ApiException(HttpStatus.CONFLICT, "employee-limit-reached", "Plan limit reached", "Upgrade the subscription plan to add more employees");
        }
        return EmployeeView.from(employeeRepository.save(EmployeeEntity.create(
                tenantId, fullName.trim(), blankToNull(specialization), workSchedule, Instant.now())));
    }

    @Transactional
    public EmployeeView updateEmployee(UUID id, String fullName, String specialization, Map<String, Object> workSchedule, boolean active) {
        var tenantId = applyTenant();
        var entity = employeeRepository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound("employee"));
        entity.update(fullName.trim(), blankToNull(specialization), workSchedule, active, Instant.now());
        return EmployeeView.from(entity);
    }

    @Transactional
    public void deactivateEmployee(UUID id) {
        var tenantId = applyTenant();
        employeeRepository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound("employee")).deactivate(Instant.now());
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceView> services(int page, int size) {
        var tenantId = applyTenant();
        var pageable = PageRequest.of(page, boundedSize(size), Sort.by("name"));
        return PageResponse.from(serviceRepository.findAllByTenantId(tenantId, pageable), ServiceView::from);
    }

    @Transactional
    public ServiceView createService(String name, int durationMinutes, BigDecimal price) {
        var tenantId = applyTenant();
        return ServiceView.from(serviceRepository.save(ServiceEntity.create(tenantId, name.trim(), durationMinutes, price, Instant.now())));
    }

    @Transactional
    public ServiceView updateService(UUID id, String name, int durationMinutes, BigDecimal price, boolean active) {
        var tenantId = applyTenant();
        var entity = serviceRepository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound("service"));
        entity.update(name.trim(), durationMinutes, price, active, Instant.now());
        return ServiceView.from(entity);
    }

    @Transactional
    public void deactivateService(UUID id) {
        var tenantId = applyTenant();
        serviceRepository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound("service")).deactivate(Instant.now());
    }

    private UUID applyTenant() {
        var tenantId = tenantContext.requireTenantId();
        rlsTenantScope.apply(tenantId);
        return tenantId;
    }

    private static int boundedSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private static String normalizePhone(String phone) {
        var trimmed = phone == null ? "" : phone.trim();
        var normalized = trimmed.startsWith("+") ? "+" + trimmed.substring(1).replaceAll("\\D", "") : trimmed.replaceAll("\\D", "");
        if (normalized.length() < 7 || normalized.length() > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid-phone", "Invalid phone", "Phone must contain 7-20 digits");
        }
        return normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException notFound(String resource) {
        return new ApiException(HttpStatus.NOT_FOUND, resource + "-not-found", "Not found", "The requested " + resource + " does not exist");
    }

    public record ClientView(UUID id, String fullName, String phone, Long telegramChatId, String notes, long version) {
        static ClientView from(ClientEntity entity) {
            return new ClientView(entity.getId(), entity.getFullName(), entity.getPhone(), entity.getTelegramChatId(), entity.getNotes(), entity.getVersion());
        }
    }

    public record EmployeeView(UUID id, String fullName, String specialization, Map<String, Object> workSchedule, boolean active, long version) {
        static EmployeeView from(EmployeeEntity entity) {
            return new EmployeeView(entity.getId(), entity.getFullName(), entity.getSpecialization(), entity.getWorkSchedule(), entity.isActive(), entity.getVersion());
        }
    }

    public record ServiceView(UUID id, String name, int durationMinutes, BigDecimal price, boolean active, long version) {
        static ServiceView from(ServiceEntity entity) {
            return new ServiceView(entity.getId(), entity.getName(), entity.getDurationMinutes(), entity.getPrice(), entity.isActive(), entity.getVersion());
        }
    }
}
