package com.mercadolibre.planning.model.me.usecases.backlog;

import com.google.common.collect.Sets;
import com.mercadolibre.planning.model.me.entities.monitor.BacklogsByDate;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;

import java.time.Duration;
import java.time.Instant;
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

    protected Instant getDateWhenLatestPhotoWasTaken(
            final List<Consolidation> consolidations,
            final Instant requestDate
    ) {
        return consolidations.stream()
                .map(Consolidation::getDate)
                .max(naturalOrder())
                .filter(date -> date.until(requestDate, ChronoUnit.MINUTES)
                        < MAX_ALLOWED_MINUTES_SHIFT)
                .orElse(requestDate);
    }

    protected List<Consolidation> truncateToHoursTheTakenOnDatesExceptFor(
            final List<Consolidation> consolidations,
            final Instant theDateThatIsKeptAsIs
    ) {
        return consolidations.stream()
                .map(cellsGroupSum -> {
                    final Instant date = theDateThatIsKeptAsIs
                            .equals(cellsGroupSum.getDate())
                            ? theDateThatIsKeptAsIs
                            : cellsGroupSum.getDate().truncatedTo(ChronoUnit.HOURS);

                    return new Consolidation(
                            date,
                            cellsGroupSum.getKeys(),
                            cellsGroupSum.getTotal()
                    );
                })
                .collect(Collectors.toList());
    }

    protected List<Consolidation> fillMissing(final List<Consolidation> consolidation,
                                              final Instant dateFrom,
                                              final Instant currentDatetime,
                                              final Function<Instant, Consolidation> backlogSupplier
    ) {
        final Set<Instant> currentDates = consolidation.stream()
                .map(Consolidation::getDate)
                .collect(Collectors.toSet());

        final long totalHours = ChronoUnit.HOURS.between(dateFrom, currentDatetime);

        final Set<Instant> allHours = IntStream.rangeClosed(0, (int) totalHours)
                .mapToObj(hours -> dateFrom.plus(Duration.ofHours(hours)))
                .collect(Collectors.toSet());
        allHours.add(currentDatetime);

        final List<Consolidation> missing = Sets.difference(allHours, currentDates).stream()
                .map(backlogSupplier)
                .collect(Collectors.toList());

        missing.addAll(consolidation);

        return missing;
    }

    protected ProcessDetail build(final ProcessName process,
                                  final Instant currentDatetime,
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
                                           final Instant currentDatetime) {

        final Instant date = currentDatetime.equals(description.getDate())
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
