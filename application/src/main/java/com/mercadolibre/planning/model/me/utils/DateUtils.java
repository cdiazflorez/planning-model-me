package com.mercadolibre.planning.model.me.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.TemporalAdjusters.previous;
import static java.time.temporal.WeekFields.SUNDAY_START;

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

    public static String[] getDatesBetween(final String[] weekOfYear) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        final ZonedDateTime dateFrom = firstDayOfYear(parseInt(weekOfYear[1]))
                .plusWeeks(parseLong(weekOfYear[0]));

        final ZonedDateTime dateTo = dateFrom.plusWeeks(1).minusMinutes(1);


        return new String[]{dateFrom.format(formatter), dateTo.format(formatter)};
    }

    private static ZonedDateTime firstDayOfYear(final int year) {
        final WeekFields fistDayRule =
                WeekFields.of(SUNDAY_START.getFirstDayOfWeek(),
                        SUNDAY_START.getMinimalDaysInFirstWeek());
        return ZonedDateTime.of(year, 1, 1, 0,0,0,0, ZoneId.of("UTC"))
                .truncatedTo(DAYS)
                .with(previous(fistDayRule.getFirstDayOfWeek()));
    }

}
