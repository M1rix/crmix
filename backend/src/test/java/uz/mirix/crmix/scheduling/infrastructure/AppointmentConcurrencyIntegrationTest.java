package uz.mirix.crmix.scheduling.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class AppointmentConcurrencyIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("crmix")
            .withUsername("postgres")
            .withPassword("postgres");

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();

    @BeforeAll
    static void setup() throws Exception {
        POSTGRES.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "postgres", "postgres");
                var statement = connection.createStatement()) {
            statement.execute("CREATE ROLE crmix_scheduler LOGIN PASSWORD 'scheduler-password' NOSUPERUSER NOCREATEDB NOCREATEROLE");
            statement.execute("GRANT USAGE ON SCHEMA public TO crmix_scheduler");
            statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO crmix_scheduler");
            statement.execute("INSERT INTO tenant(id, slug, name, business_type, subscription_status) VALUES ('" + TENANT_ID + "','race-tenant','Race Tenant','test','TRIAL')");
            statement.execute("INSERT INTO client(id, tenant_id, full_name, phone) VALUES ('" + CLIENT_ID + "','" + TENANT_ID + "','Client','+998900000001')");
            statement.execute("INSERT INTO employee(id, tenant_id, full_name, work_schedule) VALUES ('" + EMPLOYEE_ID + "','" + TENANT_ID + "','Employee','{}')");
            statement.execute("INSERT INTO service(id, tenant_id, name, duration_minutes, price) VALUES ('" + SERVICE_ID + "','" + TENANT_ID + "','Service',60,100000)");
        }
    }

    @AfterAll
    static void tearDown() {
        POSTGRES.stop();
    }

    @Test
    void databaseAllowsOnlyOneOfTwoConcurrentOverlappingAppointments() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var gate = new CountDownLatch(1);
        var slot = Instant.parse("2026-10-01T09:00:00Z");
        try {
            var first = executor.submit(() -> insertAppointmentAfter(gate, slot));
            var second = executor.submit(() -> insertAppointmentAfter(gate, slot));
            gate.countDown();
            var outcomes = java.util.List.of(first.get(), second.get());
            assertThat(outcomes.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
            assertThat(outcomes.stream().filter(value -> !value).count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean insertAppointmentAfter(CountDownLatch gate, Instant slot) throws Exception {
        gate.await();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "crmix_scheduler", "scheduler-password");
                var statement = connection.createStatement()) {
            statement.execute("SELECT set_config('app.tenant_id', '" + TENANT_ID + "', false)");
            statement.execute("INSERT INTO appointment(id, tenant_id, client_id, employee_id, service_id, scheduled_at, duration_minutes, status) VALUES (gen_random_uuid(),'"
                    + TENANT_ID + "','" + CLIENT_ID + "','" + EMPLOYEE_ID + "','" + SERVICE_ID + "','" + slot + "',60,'SCHEDULED')");
            return true;
        } catch (java.sql.SQLException expectedConflict) {
            return false;
        }
    }
}
