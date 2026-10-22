package za.ac.capaciti.capacitionlineregisterbackend.util;

import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class TimeUtil {

    public static final ZoneId ZONE = ZoneId.of("Africa/Johannesburg");

    private TimeUtil() {}

    public static LocalTime toLocalTime(Instant instant) {
        return instant.atZone(ZONE).toLocalTime();
    }

    public static LocalDate today() {
        return now().toLocalDate();
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now(ZONE);
    }

    public static Instant toInstant(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(ZONE).toInstant();
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now(ZONE);
    }

    public static LocalDate toLocalDate(Instant instant) {
        return instant.atZone(ZONE).toLocalDate();
    }

    public static Instant toInstant(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, ZONE).toInstant();
    }

    public static boolean isWeekday(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}
