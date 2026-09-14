package uz.mirix.crmix.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import uz.mirix.crmix.identity.domain.SubscriptionStatus;

@Entity
@Table(name = "tenant")
public class TenantEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String slug;
    @Column(nullable = false) private String name;
    @Column(name = "business_type", nullable = false) private String businessType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subscription_plan_id") private SubscriptionPlanEntity subscriptionPlan;
    @Column(name = "subscription_status", nullable = false) private String subscriptionStatus;
    @Column(name = "trial_ends_at") private Instant trialEndsAt;
    @Column(name = "subscription_expires_at") private Instant subscriptionExpiresAt;
    @Column(name = "time_zone", nullable = false) private String timeZone;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected TenantEntity() {}

    public static TenantEntity trial(String slug, String name, String businessType, SubscriptionPlanEntity plan, Instant now) {
        var tenant = new TenantEntity();
        tenant.id = UUID.randomUUID();
        tenant.slug = slug;
        tenant.name = name;
        tenant.businessType = businessType;
        tenant.subscriptionPlan = plan;
        tenant.subscriptionStatus = SubscriptionStatus.TRIAL.name();
        tenant.trialEndsAt = now.plusSeconds(14L * 24 * 60 * 60);
        tenant.timeZone = "Asia/Tashkent";
        tenant.createdAt = now;
        tenant.updatedAt = now;
        return tenant;
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public SubscriptionPlanEntity getSubscriptionPlan() { return subscriptionPlan; }
    public SubscriptionStatus getSubscriptionStatus() { return SubscriptionStatus.valueOf(subscriptionStatus); }
    public Instant getTrialEndsAt() { return trialEndsAt; }
    public Instant getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
    public String getTimeZone() { return timeZone; }

    public void activate(SubscriptionPlanEntity plan, Instant now) {
        this.subscriptionPlan = plan;
        this.subscriptionStatus = SubscriptionStatus.ACTIVE.name();
        var base = subscriptionExpiresAt != null && subscriptionExpiresAt.isAfter(now) ? subscriptionExpiresAt : now;
        this.subscriptionExpiresAt = base.plusSeconds(30L * 24 * 60 * 60);
        this.updatedAt = now;
    }

    public void suspend(Instant now) {
        this.subscriptionStatus = SubscriptionStatus.SUSPENDED.name();
        this.updatedAt = now;
    }
}
