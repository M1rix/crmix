package uz.mirix.crmix.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class TenantRlsIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("crmix")
            .withUsername("postgres")
            .withPassword("postgres");

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    @BeforeAll
    static void setup() throws SQLException {
        POSTGRES.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "postgres", "postgres");
                var statement = connection.createStatement()) {
            statement.execute("CREATE ROLE crmix_rls LOGIN PASSWORD 'rls-password' NOSUPERUSER NOCREATEDB NOCREATEROLE");
            statement.execute("GRANT USAGE ON SCHEMA public TO crmix_rls");
            statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO crmix_rls");
            statement.execute("INSERT INTO tenant(id, slug, name, business_type, subscription_status) VALUES ('" + TENANT_A
                    + "', 'tenant-a', 'Tenant A', 'test', 'TRIAL'), ('" + TENANT_B
                    + "', 'tenant-b', 'Tenant B', 'test', 'TRIAL')");
            statement.execute("INSERT INTO app_user(id, tenant_id, email, password_hash, role, full_name, is_active) VALUES "
                    + "(gen_random_uuid(), '" + TENANT_A + "', 'same@example.com', 'x', 'OWNER', 'Owner A', true), "
                    + "(gen_random_uuid(), '" + TENANT_B + "', 'same@example.com', 'x', 'OWNER', 'Owner B', true)");
        }
    }

    @AfterAll
    static void tearDown() {
        POSTGRES.stop();
    }

    @Test
    void tenantCannotReadAnotherTenantRowsEvenWithoutWhereClause() throws SQLException {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "crmix_rls", "rls-password");
                var statement = connection.createStatement()) {
            statement.execute("SELECT set_config('app.tenant_id', '" + TENANT_A + "', false)");
            try (var result = statement.executeQuery("SELECT count(*) FROM app_user")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            try (var result = statement.executeQuery("SELECT count(*) FROM app_user WHERE tenant_id = '" + TENANT_B + "'")) {
                result.next();
                assertThat(result.getInt(1)).isZero();
            }
        }
    }

    @Test
    void tenantCannotInsertRowForAnotherTenant() throws SQLException {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "crmix_rls", "rls-password");
                var statement = connection.createStatement()) {
            statement.execute("SELECT set_config('app.tenant_id', '" + TENANT_A + "', false)");
            assertThatThrownBy(() -> statement.execute("INSERT INTO app_user(id, tenant_id, email, password_hash, role, full_name, is_active) VALUES "
                            + "(gen_random_uuid(), '" + TENANT_B + "', 'forbidden@example.com', 'x', 'STAFF', 'Forbidden', true)"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
