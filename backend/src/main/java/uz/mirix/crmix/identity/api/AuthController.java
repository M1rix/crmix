package uz.mirix.crmix.identity.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.mirix.crmix.identity.application.IdentityService;
import uz.mirix.crmix.identity.application.LoginCommand;
import uz.mirix.crmix.identity.api.IdentityController.AuthResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final IdentityService identityService;

    public AuthController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthResponse.from(identityService.login(new LoginCommand(request.tenantSlug(), request.email(), request.password())));
    }

    @PostMapping("/refresh")
    AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return AuthResponse.from(identityService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        identityService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    public record LoginRequest(@NotBlank String tenantSlug, @NotBlank @Email String email, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
}
