package uz.mirix.crmix.scheduling.application;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.directory.application.PageResponse;
import uz.mirix.crmix.directory.infrastructure.persistence.ClientRepository;
import uz.mirix.crmix.directory.infrastructure.persistence.EmployeeRepository;
import uz.mirix.crmix.directory.infrastructure.persistence.ServiceRepository;
import uz.mirix.crmix.identity.infrastructure.persistence.TenantRepository;
import uz.mirix.crmix.platform.error.ApiException;
import uz.mirix.crmix.platform.tenancy.RlsTenantScope;
import uz.mirix.crmix.platform.tenancy.TenantContext;
import uz.mirix.crmix.scheduling.domain.AppointmentStateMachine;
import uz.mirix.crmix.scheduling.domain.AppointmentStatus;
import uz.mirix.crmix.scheduling.domain.WorkSchedulePolicy;
import uz.mirix.crmix.scheduling.infrastructure.persistence.AppointmentEntity;
import uz.mirix.crmix.scheduling.infrastructure.persistence.AppointmentRepository;

@Service
public class SchedulingService {
    private final TenantContext tenantContext;
    private final RlsTenantScope rlsTenantScope;
    private final TenantRepository tenantRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SchedulingService(
            TenantContext tenantContext,
            RlsTenantScope rlsTenantScope,
            TenantRepository tenantRepository,
            ClientRepository clientRepository,
            EmployeeRepository employeeRepository,
            ServiceRepository serviceRepository,
            AppointmentRepository appointmentRepository,
            ApplicationEventPublisher eventPublisher) {
        this.tenantContext = tenantContext;
        this.rlsTenantScope = rlsTenantScope;
        this.tenantRepository = tenantRepository;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentView> list(Instant from, Instant to, AppointmentStatus status, int page, int size) {
        var tenantId = applyTenant();
        if (!to.isAfter(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid-date-range", "Invalid date range", "Parameter 'to' must be after 'from'");
        }
        var pageable = PageRequest.of(page, Math.max(1, Math.min(size, 100)), Sort.by("scheduledAt"));
        return PageResponse.from(
                appointmentRepository.search(tenantId, from, to, status == null ? null : status.name(), pageable),
                AppointmentView::from);
    }

    @Transactional
    public AppointmentView create(UUID clientId, UUID employeeId, UUID serviceId, Instant scheduledAt) {
        var tenantId = applyTenant();
        var client = clientRepository.findByIdAndTenantId(clientId, tenantId).orElseThrow(() -> notFound("client"));
        var employee = employeeRepository.findByIdAndTenantId(employeeId, tenantId).orElseThrow(() -> notFound("employee"));
        var service = serviceRepository.findByIdAndTenantId(serviceId, tenantId).orElseThrow(() -> notFound("service"));
        if (!employee.isActive() || !service.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "inactive-directory-item", "Inactive resource", "Employee and service must be active");
        }
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> notFound("tenant"));
        if (!WorkSchedulePolicy.allows(employee.getWorkSchedule(), scheduledAt, service.getDurationMinutes(), ZoneId.of(tenant.getTimeZone()))) {
            throw new ApiException(HttpStatus.CONFLICT, "outside-working-hours", "Outside working hours", "The selected slot is outside the employee work schedule");
        }
        var endAt = scheduledAt.plusSeconds(service.getDurationMinutes() * 60L);
        if (appointmentRepository.existsOverlap(tenantId, employeeId, scheduledAt, endAt)) {
            throw slotConflict();
        }
        var entity = AppointmentEntity.create(tenantId, client.getId(), employee.getId(), service.getId(), scheduledAt, service.getDurationMinutes(), Instant.now());
        try {
            entity = appointmentRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw slotConflict();
        }
        eventPublisher.publishEvent(new AppointmentCreatedEvent(tenantId, entity.getId(), clientId, scheduledAt));
        return AppointmentView.from(entity);
    }

    @Transactional
    public AppointmentView changeStatus(UUID appointmentId, AppointmentStatus target, String cancellationReason) {
        var tenantId = applyTenant();
        var entity = appointmentRepository.findByIdAndTenantId(appointmentId, tenantId).orElseThrow(() -> notFound("appointment"));
        if (!AppointmentStateMachine.canTransition(entity.getStatus(), target)) {
            throw new ApiException(HttpStatus.CONFLICT, "invalid-appointment-transition", "Invalid status transition", "Cannot transition from " + entity.getStatus() + " to " + target);
        }
        if (target == AppointmentStatus.CANCELLED && (cancellationReason == null || cancellationReason.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "cancellation-reason-required", "Cancellation reason required", "Provide a cancellation reason");
        }
        var previous = entity.getStatus();
        entity.transition(target, cancellationReason, Instant.now());
        if (target == AppointmentStatus.CANCELLED && previous != AppointmentStatus.CANCELLED) {
            eventPublisher.publishEvent(new AppointmentCancelledEvent(tenantId, entity.getId(), entity.getClientId()));
        }
        return AppointmentView.from(entity);
    }

    @Transactional
    public AppointmentView reschedule(UUID appointmentId, Instant scheduledAt) {
        var tenantId = applyTenant();
        var entity = appointmentRepository.findByIdAndTenantId(appointmentId, tenantId).orElseThrow(() -> notFound("appointment"));
        if (entity.getStatus() == AppointmentStatus.CANCELLED || entity.getStatus() == AppointmentStatus.COMPLETED || entity.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new ApiException(HttpStatus.CONFLICT, "appointment-not-reschedulable", "Appointment cannot be rescheduled", "Only scheduled or confirmed appointments can move");
        }
        var employee = employeeRepository.findByIdAndTenantId(entity.getEmployeeId(), tenantId).orElseThrow(() -> notFound("employee"));
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> notFound("tenant"));
        if (!WorkSchedulePolicy.allows(employee.getWorkSchedule(), scheduledAt, entity.getDurationMinutes(), ZoneId.of(tenant.getTimeZone()))) {
            throw new ApiException(HttpStatus.CONFLICT, "outside-working-hours", "Outside working hours", "The selected slot is outside the employee work schedule");
        }
        var endAt = scheduledAt.plusSeconds(entity.getDurationMinutes() * 60L);
        if (appointmentRepository.existsOverlapExcluding(tenantId, entity.getEmployeeId(), appointmentId, scheduledAt, endAt)) {
            throw slotConflict();
        }
        entity.reschedule(scheduledAt, Instant.now());
        try {
            appointmentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw slotConflict();
        }
        return AppointmentView.from(entity);
    }

    private UUID applyTenant() {
        var tenantId = tenantContext.requireTenantId();
        rlsTenantScope.apply(tenantId);
        return tenantId;
    }

    private static ApiException slotConflict() {
        return new ApiException(HttpStatus.CONFLICT, "slot-conflict", "Slot conflict", "The employee already has an overlapping appointment");
    }

    private static ApiException notFound(String resource) {
        return new ApiException(HttpStatus.NOT_FOUND, resource + "-not-found", "Not found", "The requested " + resource + " does not exist");
    }

    public record AppointmentView(
            UUID id,
            UUID clientId,
            UUID employeeId,
            UUID serviceId,
            Instant scheduledAt,
            int durationMinutes,
            AppointmentStatus status,
            String cancellationReason,
            long version) {
        static AppointmentView from(AppointmentEntity entity) {
            return new AppointmentView(
                    entity.getId(),
                    entity.getClientId(),
                    entity.getEmployeeId(),
                    entity.getServiceId(),
                    entity.getScheduledAt(),
                    entity.getDurationMinutes(),
                    entity.getStatus(),
                    entity.getCancellationReason(),
                    entity.getVersion());
        }
    }
}
