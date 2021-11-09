package com.mercadolibre.planning.model.me.utils;

import com.mercadolibre.planning.model.me.entities.projection.dateselector.Date;
import com.mercadolibre.planning.model.me.entities.projection.dateselector.DateSelector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.TemporalAdjusters.previous;
import static java.time.temporal.WeekFields.SUNDAY_START;

public class DateUtils {

    public static final DateTimeFormatter FORMATTER_ID = ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static final DateTimeFormatter FORMATTER_NAME = ofPattern("dd/MM/yyyy");

    private DateUtils() { }

    public static final DateTimeFormatter HOUR_MINUTES_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public static final DateTimeFormatter DATE_HOUR_MINUTES_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm - dd/MM");

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
        return ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .truncatedTo(DAYS)
                .with(previous(fistDayRule.getFirstDayOfWeek()));
    }

    public static DateSelector getDateSelector(final ZonedDateTime dateFrom, final int daysToShow) {

        DateSelector dateSelector = new DateSelector("Fecha:", new Date[daysToShow + 1]);

        final ZonedDateTime utcDateNow = getCurrentUtcDate();
        final ZonedDateTime utcDateTo = utcDateNow.plusDays(daysToShow);
        int i = 0;

        while (!utcDateNow.plusDays(i).isAfter(utcDateTo)) {

            boolean isDateSelected = utcDateNow.plusDays(i).format(FORMATTER_NAME)
                    .equals(dateFrom.format(FORMATTER_NAME));

            dateSelector.dates[i] = new Date(
                    utcDateNow.plusDays(i).truncatedTo(HOURS).format(FORMATTER_ID),
                    utcDateNow.plusDays(i).format(FORMATTER_NAME),
                    isDateSelected);
            i++;
        }

        return dateSelector;
    }

    public static Integer minutesFromWeekStart(Instant instant) {
        var date = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return date.getDayOfWeek().getValue() * 1440
                + date.getHour() * 60
                + date.getMinute();
    }
}
