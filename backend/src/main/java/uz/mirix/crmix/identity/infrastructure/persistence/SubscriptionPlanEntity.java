package uz.mirix.crmix.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "subscription_plan")
public class SubscriptionPlanEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(name = "price_monthly", nullable = false) private BigDecimal priceMonthly;
    @Column(name = "max_employees", nullable = false) private int maxEmployees;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> features = new LinkedHashMap<>();
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @jakarta.persistence.Version private long version;

    protected SubscriptionPlanEntity() {}

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getPriceMonthly() { return priceMonthly; }
    public int getMaxEmployees() { return maxEmployees; }
    public Map<String, Object> getFeatures() { return features; }
}
