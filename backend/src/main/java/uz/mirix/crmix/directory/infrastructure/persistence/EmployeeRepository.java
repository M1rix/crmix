package uz.mirix.crmix.directory.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID> {
    Page<EmployeeEntity> findAllByTenantId(UUID tenantId, Pageable pageable);
    Optional<EmployeeEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    long countByTenantIdAndActiveTrue(UUID tenantId);
}
