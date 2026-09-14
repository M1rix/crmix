package uz.mirix.crmix.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppointmentStateMachineTest {
    @Test
    void followsBusinessTransitions() {
        assertThat(AppointmentStateMachine.canTransition(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)).isTrue();
        assertThat(AppointmentStateMachine.canTransition(AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED)).isTrue();
        assertThat(AppointmentStateMachine.canTransition(AppointmentStatus.SCHEDULED, AppointmentStatus.COMPLETED)).isFalse();
        assertThat(AppointmentStateMachine.canTransition(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED)).isFalse();
    }
}
