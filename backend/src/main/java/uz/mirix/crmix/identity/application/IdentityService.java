package uz.mirix.crmix.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mirix.crmix.identity.infrastructure.persistence.AppUserEntity;
import uz.mirix.crmix.identity.infrastructure.persistence.AppUserRepository;
import uz.mirix.crmix.identity.infrastructure.persistence.RefreshTokenEntity;
import uz.mirix.crmix.identity.infrastructure.persistence.RefreshTokenRepository;
import uz.mirix.crmix.identity.infrastructure.persistence.SubscriptionPlanRepository;
import uz.mirix.crmix.identity.infrastructure.persistence.TenantEntity;
import uz.mirix.crmix.identity.infrastructure.persistence.TenantRepository;
import uz.mirix.crmix.platform.error.ApiException;
import uz.mirix.crmix.platform.security.JwtService;
import uz.mirix.crmix.platform.tenancy.RlsTenantScope;

@Service
public class IdentityService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository planRepository;
    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RlsTenantScope rlsTenantScope;
    private final Duration refreshTtl;

    public IdentityService(
            TenantRepository tenantRepository,
            SubscriptionPlanRepository planRepository,
            AppUserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RlsTenantScope rlsTenantScope,
            @Value("${security.jwt.refresh-ttl:P30D}") Duration refreshTtl) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rlsTenantScope = rlsTenantScope;
        this.refreshTtl = refreshTtl;
    }

    @Transactional
    public AuthTokens register(RegisterTenantCommand command) {
        var slug = normalizeSlug(command.slug());
        if (tenantRepository.existsBySlugIgnoreCase(slug)) {
            throw new ApiException(HttpStatus.CONFLICT, "tenant-slug-conflict", "Tenant already exists", "Tenant slug is already in use");
        }
        var plan = planRepository.findByCode("STARTER").orElseThrow(() -> new IllegalStateException("STARTER plan is missing"));
        var now = Instant.now();
        var tenant = tenantRepository.save(TenantEntity.trial(slug, command.businessName().trim(), command.businessType().trim(), plan, now));
        rlsTenantScope.apply(tenant.getId());
        var owner = userRepository.save(AppUserEntity.owner(
                tenant.getId(), command.email(), passwordEncoder.encode(command.password()), command.ownerFullName().trim(), command.phone(), now));
        return issuePair(owner, now);
    }

    @Transactional
    public AuthTokens login(LoginCommand command) {
        var tenant = tenantRepository.findBySlugIgnoreCase(normalizeSlug(command.tenantSlug()))
                .orElseThrow(this::invalidCredentials);
        rlsTenantScope.apply(tenant.getId());
        var user = userRepository.findByTenantIdAndEmailIgnoreCase(tenant.getId(), command.email())
                .orElseThrow(this::invalidCredentials);
        if (!user.isActive() || !passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issuePair(user, Instant.now());
    }

    @Transactional
    public AuthTokens refresh(String refreshToken) {
        var tenantId = tenantIdFromOpaqueToken(refreshToken);
        rlsTenantScope.apply(tenantId);
        var stored = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(this::invalidRefreshToken);
        var now = Instant.now();
        if (!stored.getTenantId().equals(tenantId) || !stored.isUsableAt(now) || !stored.getUser().isActive()) {
            throw invalidRefreshToken();
        }
        stored.revoke(now);
        return issuePair(stored.getUser(), now);
    }

    @Transactional
    public void logout(String refreshToken) {
        var tenantId = tenantIdFromOpaqueToken(refreshToken);
        rlsTenantScope.apply(tenantId);
        refreshTokenRepository.findByTokenHash(hash(refreshToken)).ifPresent(token -> token.revoke(Instant.now()));
    }

    private AuthTokens issuePair(AppUserEntity user, Instant now) {
        var accessToken = jwtService.issueAccessToken(
                user.getId(), user.getTenantId(), user.getEmail(), user.getRole().name());
        var opaqueRefreshToken = user.getTenantId() + "." + randomToken();
        refreshTokenRepository.save(RefreshTokenEntity.create(
                user.getTenantId(), user, hash(opaqueRefreshToken), now.plus(refreshTtl), now));
        return new AuthTokens(
                accessToken,
                opaqueRefreshToken,
                jwtService.accessTokenExpiresInSeconds(),
                user.getTenantId(),
                user.getRole().name());
    }

    private static String normalizeSlug(String slug) {
        var normalized = slug == null ? "" : slug.trim().toLowerCase();
        if (!normalized.matches("[a-z0-9][a-z0-9-]{2,79}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid-tenant-slug", "Invalid tenant slug", "Use 3-80 lowercase letters, digits or hyphens");
        }
        return normalized;
    }

    private static UUID tenantIdFromOpaqueToken(String token) {
        try {
            var separator = token.indexOf('.');
            if (separator <= 0) {
                throw new IllegalArgumentException();
            }
            return UUID.fromString(token.substring(0, separator));
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid-refresh-token", "Invalid refresh token", "Refresh token is invalid or expired");
        }
    }

    private static String randomToken() {
        var bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Authentication failed", "Invalid tenant, email or password");
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid-refresh-token", "Invalid refresh token", "Refresh token is invalid or expired");
    }
}
