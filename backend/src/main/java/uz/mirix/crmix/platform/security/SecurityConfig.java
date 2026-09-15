package uz.mirix.crmix.platform.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TenantContextFilter tenantContextFilter, SubscriptionAccessFilter subscriptionAccessFilter, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .headers(headers -> headers
                        .contentTypeOptions(options -> {})
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/api/v1/tenants/register",
                                "/api/v1/auth/**",
                                "/api/v1/integrations/telegram/webhook",
                                "/api/v1/webhooks/payme",
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus")
                        .permitAll()
                        .requestMatchers("/actuator/metrics/**")
                        .hasRole("OWNER")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(subscriptionAccessFilter, TenantContextFilter.class);
        return http.build();
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean SecretKey jwtSecretKey(@Value("${security.jwt.secret:${JWT_SECRET:change-me-local-only-change-me-local-only}}") String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalStateException("JWT secret must contain at least 32 bytes");
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean JwtEncoder jwtEncoder(SecretKey secretKey) { return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey)); }
    @Bean JwtDecoder jwtDecoder(SecretKey secretKey) { return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build(); }

    @Bean JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {
            var role = jwt.getClaimAsString("role");
            return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        };
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:${APP_CORS_ALLOWED_ORIGINS:http://localhost:8088}}") String allowedOrigins) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-Id", "X-Telegram-Bot-Api-Secret-Token"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
