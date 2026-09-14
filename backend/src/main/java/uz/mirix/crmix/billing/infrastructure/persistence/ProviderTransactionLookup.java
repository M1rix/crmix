package uz.mirix.crmix.billing.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProviderTransactionLookup {
    private final JdbcClient jdbc;

    public ProviderTransactionLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void remember(String provider, String providerTransactionId, UUID tenantId, UUID paymentId) {
        jdbc.sql("""
                insert into payment_provider_transaction(provider, provider_transaction_id, tenant_id, payment_id)
                values (:provider, :transactionId, :tenantId, :paymentId)
                on conflict (provider, provider_transaction_id) do nothing
                """)
                .param("provider", provider)
                .param("transactionId", providerTransactionId)
                .param("tenantId", tenantId)
                .param("paymentId", paymentId)
                .update();
    }

    public Optional<Mapping> find(String provider, String providerTransactionId) {
        return jdbc.sql("""
                select tenant_id, payment_id from payment_provider_transaction
                where provider = :provider and provider_transaction_id = :transactionId
                """)
                .param("provider", provider)
                .param("transactionId", providerTransactionId)
                .query((rs, rowNum) -> new Mapping(rs.getObject("tenant_id", UUID.class), rs.getObject("payment_id", UUID.class)))
                .optional();
    }

    public List<Mapping> between(String provider, Instant from, Instant to) {
        return jdbc.sql("""
                select tenant_id, payment_id from payment_provider_transaction
                where provider = :provider and created_at >= :from and created_at < :to
                order by created_at
                """)
                .param("provider", provider)
                .param("from", from)
                .param("to", to)
                .query((rs, rowNum) -> new Mapping(rs.getObject("tenant_id", UUID.class), rs.getObject("payment_id", UUID.class)))
                .list();
    }

    public record Mapping(UUID tenantId, UUID paymentId) {}
}
