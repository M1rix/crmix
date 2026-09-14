package uz.mirix.crmix.billing.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.mirix.crmix.billing.application.BillingService;
import uz.mirix.crmix.billing.application.BillingService.PaymentView;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingService billingService;

    public BillingController(BillingService billingService) { this.billingService = billingService; }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    PaymentView create(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody CreatePaymentRequest request) {
        return billingService.createSubscriptionPayment(request.planCode(), idempotencyKey);
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    List<PaymentView> list() { return billingService.payments(); }

    public record CreatePaymentRequest(@NotBlank String planCode) {}
}
