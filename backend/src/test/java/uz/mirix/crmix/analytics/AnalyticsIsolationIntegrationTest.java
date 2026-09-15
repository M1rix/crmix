package uz.mirix.crmix.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AnalyticsIsolationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("crmix")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("security.jwt.secret", () -> "analytics-secret-that-is-longer-than-thirty-two-bytes");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void dashboardUsesOnlyCurrentTenantBusinessPayments() throws Exception {
        var tenantA = register("analytics-a-" + suffix(), "a@example.com");
        var tenantB = register("analytics-b-" + suffix(), "b@example.com");

        seedCompletedAppointment(tenantA.tenantId(), "A", 150_000);
        seedCompletedAppointment(tenantB.tenantId(), "B", 990_000);
        jdbc.update("""
                INSERT INTO payment(tenant_id, type, amount, currency, provider, idempotency_key, status)
                VALUES (?, 'SUBSCRIPTION', 777000, 'UZS', 'PAYME', ?, 'SUCCEEDED')
                """, tenantA.tenantId(), "subscription-noise-" + suffix());

        var today = LocalDate.now(ZoneId.of("Asia/Tashkent"));
        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .header("Authorization", "Bearer " + tenantA.accessToken())
                        .param("from", today.minusDays(29).toString())
                        .param("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(150000))
                .andExpect(jsonPath("$.appointments").value(1))
                .andExpect(jsonPath("$.cancellationRate").value(0.0))
                .andExpect(jsonPath("$.noShowRate").value(0.0))
                .andExpect(jsonPath("$.dailyRevenue[0].revenue").value(150000))
                .andExpect(jsonPath("$.employeeLoad[0].appointmentCount").value(1))
                .andExpect(jsonPath("$.employeeLoad[0].bookedMinutes").value(60))
                .andExpect(jsonPath("$.topServices[0].appointmentCount").value(1));
    }

    private Registration register(String slug, String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug":"%s",
                                  "businessName":"Analytics Test",
                                  "businessType":"clinic",
                                  "ownerFullName":"Owner",
                                  "email":"%s",
                                  "password":"strong-password-123"
                                }
                                """.formatted(slug, email)))
                .andExpect(status().isCreated())
                .andReturn();
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Registration(UUID.fromString(json.path("tenantId").asText()), json.path("accessToken").asText());
    }

    private void seedCompletedAppointment(UUID tenantId, String marker, long amount) {
        var clientId = UUID.randomUUID();
        var employeeId = UUID.randomUUID();
        var serviceId = UUID.randomUUID();
        var appointmentId = UUID.randomUUID();
        jdbc.update("INSERT INTO client(id, tenant_id, full_name, phone) VALUES (?, ?, ?, ?)", clientId, tenantId, "Client " + marker, "+99890" + Math.abs(clientId.hashCode()));
        jdbc.update("INSERT INTO employee(id, tenant_id, full_name) VALUES (?, ?, ?)", employeeId, tenantId, "Employee " + marker);
        jdbc.update("INSERT INTO service(id, tenant_id, name, duration_minutes, price) VALUES (?, ?, ?, 60, ?)", serviceId, tenantId, "Service " + marker, amount);
        jdbc.update("""
                INSERT INTO appointment(id, tenant_id, client_id, employee_id, service_id, scheduled_at, scheduled_end_at, duration_minutes, status)
                VALUES (?, ?, ?, ?, ?, now() - interval '1 day', now() - interval '23 hours', 60, 'COMPLETED')
                """, appointmentId, tenantId, clientId, employeeId, serviceId);
        jdbc.update("""
                INSERT INTO payment(tenant_id, appointment_id, type, amount, currency, provider, provider_state, idempotency_key, status, provider_performed_at)
                VALUES (?, ?, 'APPOINTMENT', ?, 'UZS', 'CASH', 2, ?, 'SUCCEEDED', now())
                """, tenantId, appointmentId, amount, "appointment-" + appointmentId);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record Registration(UUID tenantId, String accessToken) {}
}
