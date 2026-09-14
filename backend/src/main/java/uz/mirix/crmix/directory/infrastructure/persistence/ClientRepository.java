package uz.mirix.crmix.directory.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {
    Optional<ClientEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndPhone(UUID tenantId, String phone);

    @Query("""
            select c from ClientEntity c
            where c.tenantId = :tenantId
              and (:query is null
                   or lower(c.fullName) like lower(concat('%', :query, '%'))
                   or c.phone like concat('%', :query, '%'))
            """)
    Page<ClientEntity> search(@Param("tenantId") UUID tenantId, @Param("query") String query, Pageable pageable);
}
