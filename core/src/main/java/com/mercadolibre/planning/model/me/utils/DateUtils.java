package com.mercadolibre.planning.model.me.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;

public class DateUtils {

    public static ZonedDateTime getCurrentUtcDate() {
        return ZonedDateTime.now(UTC).withMinute(0).withSecond(0).withNano(0);
    }

    public static String getHourAndDay(final ZonedDateTime date) {
        return date.getHour() + "-" + date.getDayOfMonth();
    }

    public static ZonedDateTime convertToTimeZone(final ZoneId zoneId,
                                                  final ZonedDateTime date) {
        return date == null ? null : date.withZoneSameInstant(zoneId);
    }
}
