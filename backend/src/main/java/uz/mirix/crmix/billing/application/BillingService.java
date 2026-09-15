package uz.mirix.crmix.billing.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.billing.infrastructure.persistence.PaymentEntity;
import uz.mirix.crmix.billing.infrastructure.persistence.PaymentRepository;
import uz.mirix.crmix.identity.domain.SubscriptionStatus;
import uz.mirix.crmix.identity.infrastructure.persistence.SubscriptionPlanEntity;
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
    private final PaymeCheckoutLinkFactory checkoutLinkFactory;

    public BillingService(
            TenantContext tenantContext,
            RlsTenantScope rlsTenantScope,
            PaymentRepository paymentRepository,
            SubscriptionPlanRepository planRepository,
            TenantRepository tenantRepository,
            PaymeCheckoutLinkFactory checkoutLinkFactory) {
        this.tenantContext = tenantContext;
        this.rlsTenantScope = rlsTenantScope;
        this.paymentRepository = paymentRepository;
        this.planRepository = planRepository;
        this.tenantRepository = tenantRepository;
        this.checkoutLinkFactory = checkoutLinkFactory;
    }

    @Transactional
    public PaymentView createSubscriptionPayment(String planCode, String idempotencyKey) {
        var tenantId = applyTenant();
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid-idempotency-key", "Invalid Idempotency-Key", "Provide a non-empty Idempotency-Key up to 128 characters");
        }
        var existing = paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
        if (existing.isPresent()) return paymentView(existing.get());
        var plan = requirePlan(planCode);
        if (plan.getPriceMonthly().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "plan-not-payable", "Plan cannot be purchased", "Select a paid plan");
        }
        try {
            var payment = paymentRepository.saveAndFlush(PaymentEntity.subscription(tenantId, plan.getId(), plan.getPriceMonthly(), idempotencyKey, Instant.now()));
            return paymentView(payment);
        } catch (DataIntegrityViolationException conflict) {
            return paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                    .map(this::paymentView)
                    .orElseThrow(() -> conflict);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentView> payments() {
        var tenantId = applyTenant();
        return paymentRepository.findAllByTenantId(tenantId, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::paymentView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanView> plans() {
        return planRepository.findAll().stream()
                .sorted(Comparator.comparing(SubscriptionPlanEntity::getPriceMonthly))
                .map(PlanView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillingState state() {
        var tenantId = tenantContext.requireTenantId();
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "tenant-not-found", "Tenant not found", "Workspace does not exist"));
        return BillingState.from(tenant);
    }

    @Transactional
    public BillingState schedulePlanChange(String planCode) {
        var tenantId = tenantContext.requireTenantId();
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "tenant-not-found", "Tenant not found", "Workspace does not exist"));
        var target = requirePlan(planCode);
        var current = tenant.getSubscriptionPlan();
        if (current != null && current.getCode().equals(target.getCode())) {
            tenant.cancelScheduledPlanChange(Instant.now());
            return BillingState.from(tenant);
        }
        if (target.getPriceMonthly().compareTo(BigDecimal.ZERO) > 0
                && (current == null || target.getPriceMonthly().compareTo(current.getPriceMonthly()) >= 0)) {
            throw new ApiException(HttpStatus.CONFLICT, "payment-required", "Payment required", "Create a payment to upgrade to this plan");
        }
        tenant.schedulePlanChange(target, Instant.now());
        return BillingState.from(tenant);
    }

    @Scheduled(cron = "${billing.expiration-cron:0 15 * * * *}")
    @Transactional
    public void processExpiredSubscriptions() {
        var now = Instant.now();
        tenantRepository.findAll().forEach(tenant -> {
            var trialExpired = tenant.getSubscriptionStatus() == SubscriptionStatus.TRIAL
                    && tenant.getTrialEndsAt() != null && !tenant.getTrialEndsAt().isAfter(now);
            var subscriptionExpired = tenant.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                    && tenant.getSubscriptionExpiresAt() != null && !tenant.getSubscriptionExpiresAt().isAfter(now);
            if (trialExpired || subscriptionExpired) tenant.expireOrApplyScheduledPlan(now);
        });
    }

    private PaymentView paymentView(PaymentEntity entity) {
        var checkoutUrl = entity.getStatus() == uz.mirix.crmix.billing.domain.PaymentStatus.PENDING
                ? checkoutLinkFactory.create(entity.getTenantId(), entity.getId(), entity.getAmount())
                : null;
        return new PaymentView(
                entity.getId(), entity.getTenantId(), entity.getSubscriptionPlanId(), entity.getAmount(), entity.getCurrency(),
                "PAYME", entity.getStatus().name(), entity.getProviderTransactionId(), checkoutUrl, entity.getCreatedAt());
    }

    private UUID applyTenant() {
        var tenantId = tenantContext.requireTenantId();
        rlsTenantScope.apply(tenantId);
        return tenantId;
    }

    private SubscriptionPlanEntity requirePlan(String planCode) {
        return planRepository.findByCode(planCode == null ? "" : planCode.trim().toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "plan-not-found", "Plan not found", "Unknown subscription plan"));
    }

    public record PaymentView(
            UUID id,
            UUID tenantId,
            UUID planId,
            BigDecimal amount,
            String currency,
            String provider,
            String status,
            String providerTransactionId,
            String checkoutUrl,
            Instant createdAt) {}

    public record PlanView(UUID id, String code, String name, BigDecimal priceMonthly, int maxEmployees, java.util.Map<String, Object> features) {
        static PlanView from(SubscriptionPlanEntity plan) {
            return new PlanView(plan.getId(), plan.getCode(), plan.getName(), plan.getPriceMonthly(), plan.getMaxEmployees(), plan.getFeatures());
        }
    }

    public record BillingState(String status, String currentPlan, String nextPlan, Instant trialEndsAt, Instant subscriptionExpiresAt) {
        static BillingState from(uz.mirix.crmix.identity.infrastructure.persistence.TenantEntity tenant) {
            return new BillingState(
                    tenant.getSubscriptionStatus().name(),
                    tenant.getSubscriptionPlan() == null ? null : tenant.getSubscriptionPlan().getCode(),
                    tenant.getNextSubscriptionPlan() == null ? null : tenant.getNextSubscriptionPlan().getCode(),
                    tenant.getTrialEndsAt(),
                    tenant.getSubscriptionExpiresAt());
        }
    }
}
