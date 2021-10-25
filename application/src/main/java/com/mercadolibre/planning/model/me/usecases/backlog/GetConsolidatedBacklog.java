package com.mercadolibre.planning.model.me.usecases.backlog;

import com.google.common.collect.Sets;
import com.mercadolibre.planning.model.me.entities.monitor.BacklogsByDate;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.utils.DateUtils;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Comparator.naturalOrder;

abstract class GetConsolidatedBacklog {

    private static final long MAX_ALLOWED_MINUTES_SHIFT = 5L;

    protected ZonedDateTime currentDatetime(final List<Backlog> backlogs) {
        final ZonedDateTime now = DateUtils.getCurrentUtcDateTime();
        return backlogs.stream()
                .map(Backlog::getDate)
                .max(naturalOrder())
                .filter(date -> date.until(now, ChronoUnit.MINUTES) < MAX_ALLOWED_MINUTES_SHIFT)
                .orElse(now);
    }

    protected List<Backlog> truncateHours(final List<Backlog> backlogs,
                                          final ZonedDateTime currentDatetime) {
        return backlogs.stream()
                .map(backlog -> {
                            final ZonedDateTime date = currentDatetime.equals(backlog.getDate())
                                    ? currentDatetime
                                    : backlog.getDate().truncatedTo(ChronoUnit.HOURS);

                            return new Backlog(date, backlog.getKeys(), backlog.getTotal());
                        }
                )
                .collect(Collectors.toList());
    }

    protected List<Backlog> fillMissing(final List<Backlog> backlog,
                                        final ZonedDateTime dateFrom,
                                        final ZonedDateTime currentDatetime,
                                        final Function<ZonedDateTime, Backlog> backlogSupplier) {

        final Set<ZonedDateTime> currentDates = backlog.stream()
                .map(Backlog::getDate)
                .collect(Collectors.toSet());

        final long totalHours = ChronoUnit.HOURS.between(dateFrom, currentDatetime);

        final Set<ZonedDateTime> allHours = IntStream.rangeClosed(0, (int) totalHours)
                .mapToObj(dateFrom::plusHours)
                .collect(Collectors.toSet());
        allHours.add(currentDatetime);

        final List<Backlog> missing = Sets.difference(allHours, currentDates).stream()
                .map(backlogSupplier)
                .collect(Collectors.toList());

        missing.addAll(backlog);

        return missing;
    }

    protected ProcessDetail build(final ProcessName process,
                                  final ZonedDateTime currentDatetime,
                                  final List<BacklogStatsByDate> backlogs) {

        final List<BacklogsByDate> backlog = backlogs
                .stream()
                .map(stats -> toBacklogByDate(stats, currentDatetime))
                .sorted(Comparator.comparing(BacklogsByDate::getDate))
                .collect(Collectors.toList());

        final UnitMeasure totals = backlog.stream()
                .filter(b -> b.getDate().equals(currentDatetime))
                .findFirst()
                .map(BacklogsByDate::getCurrent)
                .map(measure -> new UnitMeasure(
                        measure.getUnits(),
                        measure.getMinutes() == null ? Integer.valueOf(0) : measure.getMinutes()))
                .orElse(new UnitMeasure(0, 0));

        return new ProcessDetail(process.getName(), totals, backlog);
    }

    private BacklogsByDate toBacklogByDate(final BacklogStatsByDate description,
                                           final ZonedDateTime currentDatetime) {

        final ZonedDateTime date = currentDatetime.equals(description.getDate())
                ? currentDatetime
                : description.getDate().truncatedTo(ChronoUnit.HOURS);

        return BacklogsByDate.builder()
                .date(date)
                .current(description.getTotal())
                .historical(description.getHistorical())
                .minLimit(description.getMinLimit())
                .maxLimit(description.getMaxLimit())
                .build();
    }
}
