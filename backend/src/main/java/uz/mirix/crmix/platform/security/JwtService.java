package uz.mirix.crmix.platform.security;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final Duration accessTtl;

    public JwtService(JwtEncoder encoder, @Value("${security.jwt.access-ttl:PT15M}") Duration accessTtl) {
        this.encoder = encoder;
        this.accessTtl = accessTtl;
    }

    public String issueAccessToken(UUID userId, UUID tenantId, String email, String role) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("crmix")
                .issuedAt(now)
                .expiresAt(now.plus(accessTtl))
                .subject(userId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("email", email)
                .claim("role", role)
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long accessTokenExpiresInSeconds() {
        return accessTtl.toSeconds();
    }
}
