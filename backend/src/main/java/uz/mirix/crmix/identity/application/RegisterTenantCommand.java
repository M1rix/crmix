package uz.mirix.crmix.identity.application;

public record RegisterTenantCommand(
        String slug,
        String businessName,
        String businessType,
        String ownerFullName,
        String email,
        String password,
        String phone) {}
