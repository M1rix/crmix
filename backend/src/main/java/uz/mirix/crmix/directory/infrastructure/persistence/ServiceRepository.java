package uz.mirix.crmix.directory.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID> {
    Page<ServiceEntity> findAllByTenantId(UUID tenantId, Pageable pageable);
    Optional<ServiceEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
