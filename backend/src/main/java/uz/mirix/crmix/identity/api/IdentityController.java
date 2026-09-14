package uz.mirix.crmix.identity.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.mirix.crmix.identity.application.AuthTokens;
import uz.mirix.crmix.identity.application.IdentityService;
import uz.mirix.crmix.identity.application.RegisterTenantCommand;

@RestController
@RequestMapping("/api/v1/tenants")
public class IdentityController {
    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        var tokens = identityService.register(new RegisterTenantCommand(
                request.slug(), request.businessName(), request.businessType(), request.ownerFullName(), request.email(), request.password(), request.phone()));
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + tokens.tenantId())).body(AuthResponse.from(tokens));
    }

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{2,79}") String slug,
            @NotBlank @Size(max = 160) String businessName,
            @NotBlank @Size(max = 80) String businessType,
            @NotBlank @Size(max = 160) String ownerFullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 100) String password,
            @Size(max = 40) String phone) {}

    public record AuthResponse(String accessToken, String refreshToken, long expiresIn, UUID tenantId, String role) {
        static AuthResponse from(AuthTokens tokens) {
            return new AuthResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn(), tokens.tenantId(), tokens.role());
        }
    }
}
