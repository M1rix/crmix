package uz.mirix.crmix.billing.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.mirix.crmix.billing.application.BillingService;
import uz.mirix.crmix.billing.application.BillingService.BillingState;
import uz.mirix.crmix.billing.application.BillingService.PaymentView;
import uz.mirix.crmix.billing.application.BillingService.PlanView;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingService billingService;

    public BillingController(BillingService billingService) { this.billingService = billingService; }

    @GetMapping("/plans")
    List<PlanView> plans() { return billingService.plans(); }

    @GetMapping("/state")
    BillingState state() { return billingService.state(); }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    PaymentView create(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody CreatePaymentRequest request) {
        return billingService.createSubscriptionPayment(request.planCode(), idempotencyKey);
    }

    @PostMapping("/appointment-payments")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','STAFF')")
    PaymentView recordAppointmentPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AppointmentPaymentRequest request) {
        return billingService.recordAppointmentPayment(request.appointmentId(), request.amount(), request.paymentMethod(), idempotencyKey);
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    List<PaymentView> list() { return billingService.payments(); }

    @PostMapping("/plan-change")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    BillingState schedulePlanChange(@Valid @RequestBody PlanChangeRequest request) {
        return billingService.schedulePlanChange(request.planCode());
    }

    public record CreatePaymentRequest(@NotBlank String planCode) {}
    public record PlanChangeRequest(@NotBlank String planCode) {}
    public record AppointmentPaymentRequest(
            @NotNull UUID appointmentId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp = "CASH|CARD|BANK_TRANSFER") String paymentMethod) {}
}
