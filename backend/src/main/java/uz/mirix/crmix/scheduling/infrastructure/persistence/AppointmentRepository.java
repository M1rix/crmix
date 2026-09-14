package uz.mirix.crmix.scheduling.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {
    Optional<AppointmentEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            select a from AppointmentEntity a
            where a.tenantId = :tenantId
              and a.scheduledAt >= :from and a.scheduledAt < :to
              and (:status is null or a.status = :status)
            """)
    Page<AppointmentEntity> search(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("status") String status,
            Pageable pageable);

    @Query(value = """
            select exists(
              select 1 from appointment
              where tenant_id = :tenantId
                and employee_id = :employeeId
                and status not in ('CANCELLED','NO_SHOW')
                and scheduled_at < :endAt
                and scheduled_end_at > :startAt
            )
            """, nativeQuery = true)
    boolean existsOverlap(
            @Param("tenantId") UUID tenantId,
            @Param("employeeId") UUID employeeId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt);

    @Query(value = """
            select exists(
              select 1 from appointment
              where tenant_id = :tenantId
                and employee_id = :employeeId
                and id <> :appointmentId
                and status not in ('CANCELLED','NO_SHOW')
                and scheduled_at < :endAt
                and scheduled_end_at > :startAt
            )
            """, nativeQuery = true)
    boolean existsOverlapExcluding(
            @Param("tenantId") UUID tenantId,
            @Param("employeeId") UUID employeeId,
            @Param("appointmentId") UUID appointmentId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt);
}
