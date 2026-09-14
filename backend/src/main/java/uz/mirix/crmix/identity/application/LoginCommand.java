package uz.mirix.crmix.identity.application;

public record LoginCommand(String tenantSlug, String email, String password) {}
