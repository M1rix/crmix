package uz.mirix.crmix.billing.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.billing.domain.PaymentStatus;
import uz.mirix.crmix.billing.infrastructure.persistence.PaymentEntity;
import uz.mirix.crmix.billing.infrastructure.persistence.PaymentRepository;
import uz.mirix.crmix.billing.infrastructure.persistence.ProviderTransactionLookup;
import uz.mirix.crmix.identity.infrastructure.persistence.SubscriptionPlanRepository;
import uz.mirix.crmix.identity.infrastructure.persistence.TenantRepository;
import uz.mirix.crmix.platform.tenancy.RlsTenantScope;

@Service
public class PaymeMerchantService {
    private static final String PROVIDER = "PAYME";
    private final PaymentRepository paymentRepository;
    private final ProviderTransactionLookup providerLookup;
    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository planRepository;
    private final RlsTenantScope rlsTenantScope;

    public PaymeMerchantService(PaymentRepository paymentRepository, ProviderTransactionLookup providerLookup, TenantRepository tenantRepository, SubscriptionPlanRepository planRepository, RlsTenantScope rlsTenantScope) {
        this.paymentRepository = paymentRepository;
        this.providerLookup = providerLookup;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.rlsTenantScope = rlsTenantScope;
    }

    @Transactional
    public Object handle(String method, JsonNode params) {
        return switch (method) {
            case "CheckPerformTransaction" -> checkPerform(params);
            case "CreateTransaction" -> create(params);
            case "PerformTransaction" -> perform(params);
            case "CancelTransaction" -> cancel(params);
            case "CheckTransaction" -> check(params);
            case "GetStatement" -> statement(params);
            default -> throw new PaymeRpcException(-32601, "Method not found", method);
        };
    }

    private Map<String, Object> checkPerform(JsonNode params) {
        var payment = accountPayment(params, false);
        verifyAmount(payment, params.path("amount").asLong(-1));
        if (payment.getStatus() != PaymentStatus.PENDING) throw impossible();
        return Map.of("allow", true);
    }

    private Map<String, Object> create(JsonNode params) {
        var transactionId = requiredText(params, "id");
        var existing = providerLookup.find(PROVIDER, transactionId);
        if (existing.isPresent()) return createResult(load(existing.get()));
        var payment = accountPayment(params, true);
        verifyAmount(payment, params.path("amount").asLong(-1));
        if (payment.getStatus() != PaymentStatus.PENDING) throw impossible();
        if (payment.getProviderTransactionId() != null && !payment.getProviderTransactionId().equals(transactionId)) throw impossible();
        var providerTime = Instant.ofEpochMilli(params.path("time").asLong(System.currentTimeMillis()));
        payment.createProviderTransaction(transactionId, providerTime, Instant.now());
        paymentRepository.flush();
        providerLookup.remember(PROVIDER, transactionId, payment.getTenantId(), payment.getId());
        return createResult(payment);
    }

    private Map<String, Object> perform(JsonNode params) {
        var payment = loadByProviderId(requiredText(params, "id"));
        if (payment.getStatus() == PaymentStatus.CANCELLED) throw impossible();
        if (payment.getStatus() == PaymentStatus.PENDING) {
            var now = Instant.now();
            payment.perform(now);
            if (payment.getSubscriptionPlanId() != null) {
                var tenant = tenantRepository.findById(payment.getTenantId()).orElseThrow(() -> new PaymeRpcException(-32400, "Tenant missing"));
                var plan = planRepository.findById(payment.getSubscriptionPlanId()).orElseThrow(() -> new PaymeRpcException(-32400, "Plan missing"));
                tenant.activate(plan, now);
            }
        }
        return Map.of("transaction", payment.getId().toString(), "perform_time", millis(payment.getProviderPerformedAt()), "state", 2);
    }

    private Map<String, Object> cancel(JsonNode params) {
        var payment = loadByProviderId(requiredText(params, "id"));
        if (payment.getStatus() != PaymentStatus.CANCELLED) {
            var wasSucceeded = payment.getStatus() == PaymentStatus.SUCCEEDED;
            payment.cancel(params.path("reason").asInt(0), Instant.now());
            if (wasSucceeded) tenantRepository.findById(payment.getTenantId()).ifPresent(tenant -> tenant.suspend(Instant.now()));
        }
        return Map.of("transaction", payment.getId().toString(), "cancel_time", millis(payment.getProviderCancelledAt()), "state", payment.getProviderState());
    }

    private Map<String, Object> check(JsonNode params) {
        return checkResult(loadByProviderId(requiredText(params, "id")));
    }

    private Map<String, Object> statement(JsonNode params) {
        var from = Instant.ofEpochMilli(params.path("from").asLong(0));
        var to = Instant.ofEpochMilli(params.path("to").asLong(System.currentTimeMillis()));
        var transactions = new ArrayList<Map<String, Object>>();
        for (var mapping : providerLookup.between(PROVIDER, from, to)) transactions.add(checkResult(load(mapping)));
        return Map.of("transactions", transactions);
    }

    private PaymentEntity accountPayment(JsonNode params, boolean locked) {
        var account = params.path("account");
        try {
            var tenantId = UUID.fromString(account.path("tenant_id").asText());
            var paymentId = UUID.fromString(account.path("payment_id").asText());
            rlsTenantScope.apply(tenantId);
            return (locked ? paymentRepository.findLockedByIdAndTenantId(paymentId, tenantId) : paymentRepository.findByIdAndTenantId(paymentId, tenantId))
                    .orElseThrow(() -> new PaymeRpcException(-31050, "Payment account not found", "payment_id"));
        } catch (IllegalArgumentException exception) {
            throw new PaymeRpcException(-31050, "Payment account not found", "payment_id");
        }
    }

    private PaymentEntity loadByProviderId(String transactionId) {
        var mapping = providerLookup.find(PROVIDER, transactionId).orElseThrow(() -> new PaymeRpcException(-31003, "Transaction not found"));
        return load(mapping);
    }

    private PaymentEntity load(ProviderTransactionLookup.Mapping mapping) {
        rlsTenantScope.apply(mapping.tenantId());
        return paymentRepository.findLockedByIdAndTenantId(mapping.paymentId(), mapping.tenantId())
                .orElseThrow(() -> new PaymeRpcException(-31003, "Transaction not found"));
    }

    private static void verifyAmount(PaymentEntity payment, long tiyin) {
        long expected;
        try {
            expected = payment.getAmount().movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new PaymeRpcException(-32400, "Invalid merchant amount");
        }
        if (tiyin != expected) throw new PaymeRpcException(-31001, "Incorrect amount");
    }

    private static Map<String, Object> createResult(PaymentEntity payment) {
        return Map.of("create_time", millis(payment.getProviderCreatedAt()), "transaction", payment.getId().toString(), "state", payment.getProviderState());
    }

    private static Map<String, Object> checkResult(PaymentEntity payment) {
        var result = new LinkedHashMap<String, Object>();
        result.put("create_time", millis(payment.getProviderCreatedAt()));
        result.put("perform_time", millis(payment.getProviderPerformedAt()));
        result.put("cancel_time", millis(payment.getProviderCancelledAt()));
        result.put("transaction", payment.getId().toString());
        result.put("state", payment.getProviderState() == null ? 0 : payment.getProviderState());
        result.put("reason", payment.getProviderReason());
        return result;
    }

    private static long millis(Instant value) { return value == null ? 0 : value.toEpochMilli(); }
    private static String requiredText(JsonNode node, String field) {
        var value = node.path(field).asText();
        if (value.isBlank()) throw new PaymeRpcException(-32600, "Missing field: " + field, field);
        return value;
    }
    private static PaymeRpcException impossible() { return new PaymeRpcException(-31008, "Operation is not allowed in current state"); }
}
