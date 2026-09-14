package uz.mirix.crmix.scheduling.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public final class WorkSchedulePolicy {
    private WorkSchedulePolicy() {}

    public static boolean allows(Map<String, Object> schedule, Instant start, int durationMinutes, ZoneId zoneId) {
        if (schedule == null || schedule.isEmpty()) {
            return true;
        }
        var localStart = start.atZone(zoneId);
        var localEnd = localStart.plusMinutes(durationMinutes);
        if (!localStart.toLocalDate().equals(localEnd.toLocalDate())) {
            return false;
        }
        var rawWindows = schedule.get(localStart.getDayOfWeek().name());
        if (rawWindows == null) {
            return false;
        }
        var windows = rawWindows instanceof List<?> list ? list : List.of(rawWindows);
        for (var raw : windows) {
            if (!(raw instanceof Map<?, ?> window)) {
                continue;
            }
            var startValue = window.get("start");
            var endValue = window.get("end");
            if (startValue == null || endValue == null) {
                continue;
            }
            var windowStart = LocalTime.parse(startValue.toString());
            var windowEnd = LocalTime.parse(endValue.toString());
            if (!localStart.toLocalTime().isBefore(windowStart) && !localEnd.toLocalTime().isAfter(windowEnd)) {
                return true;
            }
        }
        return false;
    }
}
