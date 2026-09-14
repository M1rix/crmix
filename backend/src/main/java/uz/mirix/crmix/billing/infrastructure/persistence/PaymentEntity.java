package uz.mirix.crmix.billing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import uz.mirix.crmix.billing.domain.PaymentStatus;

@Entity
@Table(name = "payment")
public class PaymentEntity {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "appointment_id") private UUID appointmentId;
    @Column(name = "subscription_plan_id") private UUID subscriptionPlanId;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private BigDecimal amount;
    @Column(nullable = false) private String currency;
    @Column(nullable = false) private String provider;
    @Column(name = "provider_transaction_id") private String providerTransactionId;
    @Column(name = "provider_state") private Integer providerState;
    @Column(name = "idempotency_key", nullable = false) private String idempotencyKey;
    @Column(nullable = false) private String status;
    @Column(name = "provider_created_at") private Instant providerCreatedAt;
    @Column(name = "provider_performed_at") private Instant providerPerformedAt;
    @Column(name = "provider_cancelled_at") private Instant providerCancelledAt;
    @Column(name = "provider_reason") private Integer providerReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected PaymentEntity() {}

    public static PaymentEntity subscription(UUID tenantId, UUID planId, BigDecimal amount, String idempotencyKey, Instant now) {
        var entity = new PaymentEntity();
        entity.id = UUID.randomUUID();
        entity.tenantId = tenantId;
        entity.subscriptionPlanId = planId;
        entity.type = "SUBSCRIPTION";
        entity.amount = amount;
        entity.currency = "UZS";
        entity.provider = "PAYME";
        entity.idempotencyKey = idempotencyKey;
        entity.status = PaymentStatus.PENDING.name();
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void createProviderTransaction(String transactionId, Instant providerTime, Instant now) {
        if (providerTransactionId != null && !providerTransactionId.equals(transactionId)) {
            throw new IllegalStateException("Payment already belongs to another provider transaction");
        }
        providerTransactionId = transactionId;
        providerState = 1;
        providerCreatedAt = providerTime;
        updatedAt = now;
    }

    public void perform(Instant now) {
        status = PaymentStatus.SUCCEEDED.name();
        providerState = 2;
        providerPerformedAt = now;
        updatedAt = now;
    }

    public void cancel(int reason, Instant now) {
        var wasPerformed = getStatus() == PaymentStatus.SUCCEEDED;
        status = PaymentStatus.CANCELLED.name();
        providerState = wasPerformed ? -2 : -1;
        providerCancelledAt = now;
        providerReason = reason;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSubscriptionPlanId() { return subscriptionPlanId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public Integer getProviderState() { return providerState; }
    public PaymentStatus getStatus() { return PaymentStatus.valueOf(status); }
    public Instant getProviderCreatedAt() { return providerCreatedAt; }
    public Instant getProviderPerformedAt() { return providerPerformedAt; }
    public Instant getProviderCancelledAt() { return providerCancelledAt; }
    public Integer getProviderReason() { return providerReason; }
    public Instant getCreatedAt() { return createdAt; }
}
