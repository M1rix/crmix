package uz.mirix.crmix.billing.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.billing.infrastructure.persistence.PaymentEntity;
import uz.mirix.crmix.billing.infrastructure.persistence.PaymentRepository;
import uz.mirix.crmix.identity.domain.SubscriptionStatus;
import uz.mirix.crmix.identity.infrastructure.persistence.SubscriptionPlanRepository;
import uz.mirix.crmix.identity.infrastructure.persistence.TenantRepository;
import uz.mirix.crmix.platform.error.ApiException;
import uz.mirix.crmix.platform.tenancy.RlsTenantScope;
import uz.mirix.crmix.platform.tenancy.TenantContext;

@Service
public class BillingService {
    private final TenantContext tenantContext;
    private final RlsTenantScope rlsTenantScope;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final TenantRepository tenantRepository;

    public BillingService(TenantContext tenantContext, RlsTenantScope rlsTenantScope, PaymentRepository paymentRepository, SubscriptionPlanRepository planRepository, TenantRepository tenantRepository) {
        this.tenantContext = tenantContext;
        this.rlsTenantScope = rlsTenantScope;
        this.paymentRepository = paymentRepository;
        this.planRepository = planRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public PaymentView createSubscriptionPayment(String planCode, String idempotencyKey) {
        var tenantId = tenantContext.requireTenantId();
        rlsTenantScope.apply(tenantId);
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid-idempotency-key", "Invalid Idempotency-Key", "Provide a non-empty Idempotency-Key up to 128 characters");
        }
        var existing = paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
        if (existing.isPresent()) {
            return PaymentView.from(existing.get());
        }
        var plan = planRepository.findByCode(planCode.toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "plan-not-found", "Plan not found", "Unknown subscription plan"));
        if (plan.getPriceMonthly().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "plan-not-payable", "Plan cannot be purchased", "Select a paid plan");
        }
        return PaymentView.from(paymentRepository.save(PaymentEntity.subscription(tenantId, plan.getId(), plan.getPriceMonthly(), idempotencyKey, Instant.now())));
    }

    @Transactional(readOnly = true)
    public List<PaymentView> payments() {
        var tenantId = tenantContext.requireTenantId();
        rlsTenantScope.apply(tenantId);
        return paymentRepository.findAllByTenantId(tenantId, Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(PaymentView::from).toList();
    }

    @Scheduled(cron = "${billing.expiration-cron:0 15 * * * *}")
    @Transactional
    public void suspendExpiredSubscriptions() {
        var now = Instant.now();
        tenantRepository.findAll().forEach(tenant -> {
            var trialExpired = tenant.getSubscriptionStatus() == SubscriptionStatus.TRIAL
                    && tenant.getTrialEndsAt() != null && !tenant.getTrialEndsAt().isAfter(now);
            var subscriptionExpired = tenant.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                    && tenant.getSubscriptionExpiresAt() != null && !tenant.getSubscriptionExpiresAt().isAfter(now);
            if (trialExpired || subscriptionExpired) tenant.suspend(now);
        });
    }

    public record PaymentView(UUID id, UUID tenantId, UUID planId, BigDecimal amount, String currency, String provider, String status, String providerTransactionId, Instant createdAt) {
        static PaymentView from(PaymentEntity entity) {
            return new PaymentView(entity.getId(), entity.getTenantId(), entity.getSubscriptionPlanId(), entity.getAmount(), entity.getCurrency(), "PAYME", entity.getStatus().name(), entity.getProviderTransactionId(), entity.getCreatedAt());
        }
    }
}
