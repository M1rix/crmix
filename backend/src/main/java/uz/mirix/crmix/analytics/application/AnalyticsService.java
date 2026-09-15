package uz.mirix.crmix.analytics.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.identity.infrastructure.persistence.TenantRepository;
import uz.mirix.crmix.platform.error.ApiException;
import uz.mirix.crmix.platform.tenancy.RlsTenantScope;
import uz.mirix.crmix.platform.tenancy.TenantContext;

@Service
public class AnalyticsService {
    private final TenantContext tenantContext;
    private final RlsTenantScope rlsTenantScope;
    private final TenantRepository tenantRepository;
    private final JdbcTemplate jdbc;

    public AnalyticsService(TenantContext tenantContext, RlsTenantScope rlsTenantScope, TenantRepository tenantRepository, JdbcTemplate jdbc) {
        this.tenantContext = tenantContext;
        this.rlsTenantScope = rlsTenantScope;
        this.tenantRepository = tenantRepository;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || from.plusYears(2).isBefore(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid-period", "Invalid analytics period", "Use a valid period up to two years");
        }
        var tenantId = tenantContext.requireTenantId();
        rlsTenantScope.apply(tenantId);
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "tenant-not-found", "Tenant not found", "Workspace does not exist"));
        var zone = ZoneId.of(tenant.getTimeZone());
        var fromTimestamp = from.atStartOfDay(zone).toInstant().atOffset(ZoneOffset.UTC);
        var toExclusiveTimestamp = to.plusDays(1).atStartOfDay(zone).toInstant().atOffset(ZoneOffset.UTC);

        var revenue = jdbc.queryForObject("""
                SELECT COALESCE(SUM(amount), 0)
                FROM payment
                WHERE tenant_id = ? AND type = 'APPOINTMENT' AND status = 'SUCCEEDED'
                  AND created_at >= ? AND created_at < ?
                """, BigDecimal.class, tenantId, fromTimestamp, toExclusiveTimestamp);

        var stats = jdbc.queryForObject("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled,
                       COUNT(*) FILTER (WHERE status = 'NO_SHOW') AS no_show
                FROM appointment
                WHERE tenant_id = ? AND scheduled_at >= ? AND scheduled_at < ?
                """, (rs, rowNum) -> new AppointmentStats(rs.getLong("total"), rs.getLong("cancelled"), rs.getLong("no_show")), tenantId, fromTimestamp, toExclusiveTimestamp);

        var dailyRevenue = jdbc.query("""
                SELECT DATE(timezone(?, created_at)) AS day, COALESCE(SUM(amount), 0) AS revenue
                FROM payment
                WHERE tenant_id = ? AND type = 'APPOINTMENT' AND status = 'SUCCEEDED'
                  AND created_at >= ? AND created_at < ?
                GROUP BY 1
                ORDER BY day
                """, (rs, rowNum) -> new DailyRevenue(rs.getDate("day").toLocalDate(), rs.getBigDecimal("revenue")),
                zone.getId(), tenantId, fromTimestamp, toExclusiveTimestamp);

        var employeeLoad = jdbc.query("""
                SELECT e.id, e.full_name,
                       COUNT(a.id) AS appointment_count,
                       COALESCE(SUM(a.duration_minutes), 0) AS booked_minutes
                FROM employee e
                LEFT JOIN appointment a
                  ON a.employee_id = e.id
                 AND a.tenant_id = e.tenant_id
                 AND a.scheduled_at >= ? AND a.scheduled_at < ?
                 AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
                WHERE e.tenant_id = ? AND e.is_active = TRUE
                GROUP BY e.id, e.full_name
                ORDER BY booked_minutes DESC, e.full_name
                """, (rs, rowNum) -> new EmployeeLoad(
                        rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getLong("appointment_count"), rs.getLong("booked_minutes")),
                fromTimestamp, toExclusiveTimestamp, tenantId);

        var topServices = jdbc.query("""
                SELECT s.id, s.name, COUNT(a.id) AS appointment_count
                FROM service s
                LEFT JOIN appointment a
                  ON a.service_id = s.id
                 AND a.tenant_id = s.tenant_id
                 AND a.scheduled_at >= ? AND a.scheduled_at < ?
                 AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
                WHERE s.tenant_id = ?
                GROUP BY s.id, s.name
                ORDER BY appointment_count DESC, s.name
                LIMIT 10
                """, (rs, rowNum) -> new TopService(rs.getObject("id", UUID.class), rs.getString("name"), rs.getLong("appointment_count")),
                fromTimestamp, toExclusiveTimestamp, tenantId);

        var total = stats == null ? 0 : stats.total();
        var cancelled = stats == null ? 0 : stats.cancelled();
        var noShow = stats == null ? 0 : stats.noShow();
        return new Dashboard(
                from,
                to,
                revenue == null ? BigDecimal.ZERO : revenue,
                total,
                percentage(cancelled, total),
                percentage(noShow, total),
                dailyRevenue,
                employeeLoad,
                topServices);
    }

    private static double percentage(long value, long total) {
        return total == 0 ? 0D : Math.round((value * 10000D) / total) / 100D;
    }

    private record AppointmentStats(long total, long cancelled, long noShow) {}

    public record Dashboard(
            LocalDate from,
            LocalDate to,
            BigDecimal revenue,
            long appointments,
            double cancellationRate,
            double noShowRate,
            List<DailyRevenue> dailyRevenue,
            List<EmployeeLoad> employeeLoad,
            List<TopService> topServices) {}

    public record DailyRevenue(LocalDate day, BigDecimal revenue) {}
    public record EmployeeLoad(UUID employeeId, String employeeName, long appointmentCount, long bookedMinutes) {}
    public record TopService(UUID serviceId, String serviceName, long appointmentCount) {}
}
