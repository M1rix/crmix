package uz.mirix.crmix.scheduling.domain;

import java.util.EnumSet;
import java.util.Map;

public final class AppointmentStateMachine {
    private static final Map<AppointmentStatus, EnumSet<AppointmentStatus>> ALLOWED = Map.of(
            AppointmentStatus.SCHEDULED, EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
            AppointmentStatus.CONFIRMED, EnumSet.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
            AppointmentStatus.COMPLETED, EnumSet.noneOf(AppointmentStatus.class),
            AppointmentStatus.CANCELLED, EnumSet.noneOf(AppointmentStatus.class),
            AppointmentStatus.NO_SHOW, EnumSet.noneOf(AppointmentStatus.class));

    private AppointmentStateMachine() {}

    public static boolean canTransition(AppointmentStatus from, AppointmentStatus to) {
        return from == to || ALLOWED.get(from).contains(to);
    }
}
