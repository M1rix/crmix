package uz.mirix.crmix.identity.application;

import java.util.UUID;

public record AuthTokens(String accessToken, String refreshToken, long expiresIn, UUID tenantId, String role) {}
