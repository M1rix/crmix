package uz.mirix.crmix.scheduling.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.mirix.crmix.directory.application.PageResponse;
import uz.mirix.crmix.scheduling.application.SchedulingService;
import uz.mirix.crmix.scheduling.application.SchedulingService.AppointmentView;
import uz.mirix.crmix.scheduling.domain.AppointmentStatus;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {
    private final SchedulingService schedulingService;

    public AppointmentController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @GetMapping
    PageResponse<AppointmentView> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        var effectiveFrom = from == null ? Instant.now().minus(30, ChronoUnit.DAYS) : from;
        var effectiveTo = to == null ? Instant.now().plus(90, ChronoUnit.DAYS) : to;
        return schedulingService.list(effectiveFrom, effectiveTo, status, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','STAFF')")
    AppointmentView create(@Valid @RequestBody CreateAppointmentRequest request) {
        return schedulingService.create(request.clientId(), request.employeeId(), request.serviceId(), request.scheduledAt());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','STAFF')")
    AppointmentView changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return schedulingService.changeStatus(id, request.status(), request.cancellationReason());
    }

    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','STAFF')")
    AppointmentView reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleRequest request) {
        return schedulingService.reschedule(id, request.scheduledAt());
    }

    public record CreateAppointmentRequest(
            @NotNull UUID clientId,
            @NotNull UUID employeeId,
            @NotNull UUID serviceId,
            @NotNull Instant scheduledAt) {}

    public record StatusRequest(
            @NotNull AppointmentStatus status,
            @Size(max = 1000) String cancellationReason) {}

    public record RescheduleRequest(@NotNull Instant scheduledAt) {}
}
