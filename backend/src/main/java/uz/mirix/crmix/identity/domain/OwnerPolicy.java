package uz.mirix.crmix.identity.domain;

public final class OwnerPolicy {
    private OwnerPolicy() {}

    public static void ensureCanDemote(UserRole currentRole, UserRole requestedRole, long activeOwnerCount) {
        if (currentRole == UserRole.OWNER && requestedRole != UserRole.OWNER && activeOwnerCount <= 1) {
            throw new IllegalStateException("The last active OWNER cannot be demoted");
        }
    }
}
