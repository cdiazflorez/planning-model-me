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
import java.util.stream.IntStream;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.TemporalAdjusters.previous;
import static java.time.temporal.WeekFields.SUNDAY_START;

public class DateUtils {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

    public static final DateTimeFormatter HOUR_MINUTES_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static final DateTimeFormatter DATE_HOUR_MINUTES_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM");

    public static final DateTimeFormatter FORMATTER_NAME = ofPattern("dd/MM/yyyy");

    public static final DateTimeFormatter FORMATTER_ID = ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private DateUtils() {
    }

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
                .plusWeeks(parseLong(weekOfYear[0])).minusWeeks(1);

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

    public static DateSelector getDateSelector(final ZonedDateTime currentDate,
                                               final ZonedDateTime dateFromToShow,
                                               final int daysToShow) {

        final ZonedDateTime utcDateNow = currentDate.truncatedTo(DAYS);
        final Instant firstDayToShow = dateFromToShow.truncatedTo(DAYS)
                .toInstant();

        final Instant selected = utcDateNow.toInstant().isAfter(firstDayToShow)
                ? utcDateNow.toInstant()
                : firstDayToShow;

        return new DateSelector(
                "Fecha:",
                IntStream.rangeClosed(0, daysToShow)
                        .mapToObj(utcDateNow::plusDays)
                        .map(day -> new Date(
                                day.format(FORMATTER_ID),
                                day.format(FORMATTER_NAME),
                                day.toInstant().equals(selected))
                        ).toArray(Date[]::new)
        );
    }

    public static Integer minutesFromWeekStart(final Instant instant) {
        var date = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return date.getDayOfWeek().getValue() * 1440
                + date.getHour() * 60
                + date.getMinute();
    }
}
