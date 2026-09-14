package uz.mirix.crmix.billing.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    Optional<PaymentEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    Optional<PaymentEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<PaymentEntity> findAllByTenantId(UUID tenantId, Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentEntity> findLockedByIdAndTenantId(UUID id, UUID tenantId);
}
