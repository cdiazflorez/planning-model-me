package com.mercadolibre.planning.model.me.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.HOURS;

public class DateUtils {

    public static ZonedDateTime getCurrentUtcDate() {
        return ZonedDateTime.now(UTC).withMinute(0).withSecond(0).withNano(0);
    }

    public static ZonedDateTime getCurrentUtcDateTime() {
        return ZonedDateTime.now(UTC).withSecond(0).withNano(0);
    }

    public static String getHourAndDay(final ZonedDateTime date) {
        return date.getHour() + "-" + date.getDayOfMonth();
    }

    public static ZonedDateTime convertToTimeZone(final ZoneId zoneId,
                                                  final ZonedDateTime date) {
        return date == null ? null : date.withZoneSameInstant(zoneId);
    }

    public static ZonedDateTime convertToUtc(final ZonedDateTime date) {
        return date.withZoneSameInstant(UTC);
    }

    public static ZonedDateTime getNextHour(final ZonedDateTime dateTime) {
        return dateTime.truncatedTo(HOURS).plusHours(1);
    }

}
