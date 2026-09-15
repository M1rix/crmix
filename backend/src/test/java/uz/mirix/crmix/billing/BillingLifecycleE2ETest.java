package uz.mirix.crmix.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import uz.mirix.crmix.billing.application.BillingService;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BillingLifecycleE2ETest {
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
        registry.add("security.jwt.secret", () -> "e2e-secret-that-is-longer-than-thirty-two-bytes");
        registry.add("payme.login", () -> "Paycom");
        registry.add("payme.secret", () -> "test-secret");
        registry.add("billing.payme.merchant-id", () -> "merchant-test-id");
        registry.add("billing.payme.checkout-url", () -> "https://test.paycom.uz");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired BillingService billingService;

    @Test
    void trialPaymentActivationExpirationAndSuspensionAreOneConsistentFlow() throws Exception {
        var slug = "e2e-" + UUID.randomUUID().toString().substring(0, 8);
        var registration = mockMvc.perform(post("/api/v1/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug":"%s",
                                  "businessName":"E2E Clinic",
                                  "businessType":"clinic",
                                  "ownerFullName":"E2E Owner",
                                  "email":"owner@example.com",
                                  "password":"strong-password-123",
                                  "phone":"+998900000001"
                                }
                                """.formatted(slug)))
                .andExpect(status().isCreated())
                .andReturn();

        var auth = objectMapper.readTree(registration.getResponse().getContentAsString());
        var accessToken = auth.path("accessToken").asText();
        var tenantId = UUID.fromString(auth.path("tenantId").asText());
        var bearer = "Bearer " + accessToken;

        mockMvc.perform(get("/api/v1/billing/state").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRIAL"));

        var paymentResult = mockMvc.perform(post("/api/v1/billing/payments")
                        .header("Authorization", bearer)
                        .header("Idempotency-Key", "e2e-payment-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"PRO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.checkoutUrl").isNotEmpty())
                .andReturn();

        var payment = objectMapper.readTree(paymentResult.getResponse().getContentAsString());
        var paymentId = payment.path("id").asText();
        var amountTiyin = payment.path("amount").decimalValue().movePointRight(2).longValueExact();
        var providerTransaction = "0123456789abcdef01234567";
        var basic = "Basic " + Base64.getEncoder().encodeToString("Paycom:test-secret".getBytes(StandardCharsets.UTF_8));

        var createTransaction = """
                {
                  "id":1,
                  "method":"CreateTransaction",
                  "params":{
                    "id":"%s",
                    "time":%d,
                    "amount":%d,
                    "account":{"tenant_id":"%s","payment_id":"%s"}
                  }
                }
                """.formatted(providerTransaction, System.currentTimeMillis(), amountTiyin, tenantId, paymentId);

        mockMvc.perform(post("/api/v1/webhooks/payme")
                        .header("Authorization", basic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTransaction))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.state").value(1));

        mockMvc.perform(post("/api/v1/webhooks/payme")
                        .header("Authorization", basic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":2,"method":"PerformTransaction","params":{"id":"%s"}}
                                """.formatted(providerTransaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.state").value(2));

        mockMvc.perform(get("/api/v1/billing/state").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentPlan").value("PRO"));

        jdbcTemplate.update("UPDATE tenant SET subscription_expires_at = now() - interval '1 minute' WHERE id = ?", tenantId);
        billingService.processExpiredSubscriptions();

        mockMvc.perform(get("/api/v1/billing/state").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/v1/appointments").header("Authorization", bearer))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.title").value("Subscription suspended"));
    }
}
