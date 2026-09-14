package uz.mirix.crmix.directory.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.mirix.crmix.directory.application.DirectoryService;
import uz.mirix.crmix.directory.application.DirectoryService.ClientView;
import uz.mirix.crmix.directory.application.DirectoryService.EmployeeView;
import uz.mirix.crmix.directory.application.DirectoryService.ServiceView;
import uz.mirix.crmix.directory.application.PageResponse;

@RestController
@RequestMapping("/api/v1")
public class DirectoryController {
    private final DirectoryService directoryService;

    public DirectoryController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping("/clients")
    PageResponse<ClientView> clients(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return directoryService.clients(query, page, size);
    }

    @PostMapping("/clients")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    ClientView createClient(@Valid @RequestBody ClientRequest request) {
        return directoryService.createClient(request.fullName(), request.phone(), request.notes());
    }

    @PutMapping("/clients/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    ClientView updateClient(@PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
        return directoryService.updateClient(id, request.fullName(), request.phone(), request.notes());
    }

    @DeleteMapping("/clients/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    ResponseEntity<Void> deleteClient(@PathVariable UUID id) {
        directoryService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/employees")
    PageResponse<EmployeeView> employees(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return directoryService.employees(page, size);
    }

    @PostMapping("/employees")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    EmployeeView createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return directoryService.createEmployee(request.fullName(), request.specialization(), request.workSchedule());
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    EmployeeView updateEmployee(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest request) {
        return directoryService.updateEmployee(id, request.fullName(), request.specialization(), request.workSchedule(), request.active());
    }

    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    ResponseEntity<Void> deactivateEmployee(@PathVariable UUID id) {
        directoryService.deactivateEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/services")
    PageResponse<ServiceView> services(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return directoryService.services(page, size);
    }

    @PostMapping("/services")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    ServiceView createService(@Valid @RequestBody ServiceRequest request) {
        return directoryService.createService(request.name(), request.durationMinutes(), request.price());
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    ServiceView updateService(@PathVariable UUID id, @Valid @RequestBody ServiceRequest request) {
        return directoryService.updateService(id, request.name(), request.durationMinutes(), request.price(), request.active());
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    ResponseEntity<Void> deactivateService(@PathVariable UUID id) {
        directoryService.deactivateService(id);
        return ResponseEntity.noContent().build();
    }

    public record ClientRequest(
            @NotBlank @Size(max = 160) String fullName,
            @NotBlank @Size(max = 40) String phone,
            @Size(max = 4000) String notes) {}

    public record EmployeeRequest(
            @NotBlank @Size(max = 160) String fullName,
            @Size(max = 160) String specialization,
            Map<String, Object> workSchedule,
            boolean active) {}

    public record ServiceRequest(
            @NotBlank @Size(max = 160) String name,
            @Min(5) @Max(1440) int durationMinutes,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            boolean active) {}
}
