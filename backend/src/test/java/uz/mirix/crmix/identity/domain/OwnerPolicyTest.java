package uz.mirix.crmix.identity.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OwnerPolicyTest {
    @Test
    void refusesToDemoteLastOwner() {
        assertThatThrownBy(() -> OwnerPolicy.ensureCanDemote(UserRole.OWNER, UserRole.ADMIN, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last active OWNER");
    }

    @Test
    void allowsDemotionWhenAnotherOwnerExists() {
        assertThatCode(() -> OwnerPolicy.ensureCanDemote(UserRole.OWNER, UserRole.ADMIN, 2)).doesNotThrowAnyException();
    }
}
