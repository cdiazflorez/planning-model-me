package com.mercadolibre.planning.model.me.usecases.backlog;

import static java.time.ZoneOffset.UTC;
import static java.util.Comparator.naturalOrder;

import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.entities.monitor.VariablesPhoto;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class GetConsolidatedBacklog {

  private static final int GAP = 30;

  private static final long MAX_ALLOWED_MINUTES_SHIFT = 5L;

  private static final long SECOND_IN_HOUR = 3600;

  /**
   * Filters by taken on only dates on the dot and the last photo, excluding the others
   *
   * @param sumsOfCellsGroupedByTakenOnDateAndProcess the backlog photos taken between `dateFrom`
   *                                                  and `requestInstant`, with all the cells corresponding to the same photo
   *                                                  and process consolidated.
   */
  protected static List<Consolidation> filterSumsOfCellByTakenOnTheDot(List<Consolidation> sumsOfCellsGroupedByTakenOnDateAndProcess) {
    return sumsOfCellsGroupedByTakenOnDateAndProcess.stream()
        .filter(v -> (v.getDate().atZone(UTC).getMinute() < GAP || !v.isFirstPhotoOfPeriod()))
        .collect(Collectors.toList());
  }

  /**
   * Converts a backlog trajectory from units to the time needed to process them.
   */
  //TODO: DELETED PARAMETER totaledBacklogPhoto and "if" when merge with branch of details
  static Map<Instant, UnitMeasure> convertTrajectoryFromUnitsToDuration(
      final Stream<BacklogPhoto> backlogTrajectoryInUnits,
      final Stream<TotaledBacklogPhoto> totaledBacklogPhoto,
      final ThroughputTrajectory throughputTrajectory
  ) {
    if (backlogTrajectoryInUnits != null) {
      return backlogTrajectoryInUnits.collect(Collectors.toMap(
          BacklogPhoto::getTakenOn,
          item -> new UnitMeasure(
              item.getQuantity(),
              (int) ChronoUnit.MINUTES.between(
                  item.getTakenOn(),
                  throughputTrajectory.reversedIntegral(item.getTakenOn(), item.getQuantity())
              )
          ),
          (a, b) -> {
            assert a.equals(b);
            return a;
          }
      ));

    } else if (totaledBacklogPhoto != null) {
      return totaledBacklogPhoto.collect(Collectors.toMap(
          TotaledBacklogPhoto::getTakenOn,
          item -> new UnitMeasure(
              item.getQuantity(),
              (int) ChronoUnit.MINUTES.between(
                  item.getTakenOn(),
                  throughputTrajectory.reversedIntegral(item.getTakenOn(), item.getQuantity())
              )
          ),
          (a, b) -> {
            assert a.equals(b);
            return a;
          }
      ));
    } else {
      return Collections.emptyMap();
    }
  }

  /**
   * Gets, from the received consolidated backlog trajectory, the date when the latest backlog
   * photo was taken, or the {@code defaultDate} if either the received {@code consolidations}
   * list is empty or the latest photo date is more than five minutes earlier than the
   * {@code defaultDate}. TODO Explain why the second is necessary.
   *
   * @return the date mentioned date.
   */
  protected Instant getDateWhenLatestPhotoWasTaken(
      final List<Consolidation> consolidationTrajectory,
      final Instant defaultDate
  ) {
    return consolidationTrajectory.stream()
        .map(Consolidation::getDate)
        .max(naturalOrder())
        .filter(date -> date.until(defaultDate, ChronoUnit.MINUTES)
            < MAX_ALLOWED_MINUTES_SHIFT)
        .orElse(defaultDate);
  }

  protected ProcessDetail build(
      final ProcessName process,
      final Instant currentDatetime,
      final List<BacklogStatsByDate> backlogs,
      final Instant dateFrom
  ) {

    final List<VariablesPhoto> backlog = backlogs
        .stream()
        .map(stats -> toBacklogByDate(stats, currentDatetime))
        .sorted(Comparator.comparing(VariablesPhoto::getDate))
        .collect(Collectors.toList());

    final UnitMeasure totals = backlog.stream()
        .filter(b -> b.getDate().equals(currentDatetime))
        .findFirst()
        .map(VariablesPhoto::getCurrent)
        .map(measure -> new UnitMeasure(
            measure.getUnits(),
            measure.getMinutes() == null ? Integer.valueOf(0) : measure.getMinutes()))
        .orElse(new UnitMeasure(0, 0));

    final List<VariablesPhoto> filteredBacklog = backlog
        .stream()
        .filter(bck -> !bck.getDate().isBefore(dateFrom))
        .collect(Collectors.toList());

    return new ProcessDetail(process.getName(), totals, filteredBacklog);
  }

  protected Map<Instant, UnitMeasure> convertBacklogTrajectoryFromUnitToTime(final List<TotaledBacklogPhoto> totaledBacklogPhotos,
                                                                             final List<BacklogPhoto> backlogPhotos,
                                                                             final Map<Instant, Integer> throughputByHour) {

    final ThroughputTrajectory throughputTrajectory = new ThroughputTrajectory(new TreeMap<>(throughputByHour));

    var backlog = backlogPhotos != null && !backlogPhotos.isEmpty() ? backlogPhotos.stream() : null;
    var totaledBacklog = totaledBacklogPhotos != null && !totaledBacklogPhotos.isEmpty()
        ? totaledBacklogPhotos.stream() : null;

    return convertTrajectoryFromUnitsToDuration(backlog, totaledBacklog, throughputTrajectory);
  }

  private VariablesPhoto toBacklogByDate(
      final BacklogStatsByDate description,
      final Instant currentDatetime
  ) {
    final Instant date = currentDatetime.equals(description.getDate())
        ? currentDatetime
        : description.getDate().truncatedTo(ChronoUnit.HOURS);

    return VariablesPhoto.builder()
        .date(date)
        .current(description.getTotal())
        .historical(description.getHistorical())
        .build();
  }

  static class ThroughputTrajectory {
    final TreeMap<Instant, Integer> throughputByHour;

    public ThroughputTrajectory(TreeMap<Instant, Integer> throughputByHour) {
      this.throughputByHour = throughputByHour;

      if (throughputByHour.isEmpty()) {
        // An undefined throughput trajectory is interpreted as a constant infinity throughput.
        throughputByHour.put(Instant.ofEpochSecond(0), Integer.MAX_VALUE);
      } else {
        // Extrapolate the left side with the first value
        throughputByHour.put(Instant.ofEpochSecond(0), throughputByHour.firstEntry().getValue());
        // The right side is naturally extrapolated with the last value.
        // To avoid infinite recursion when the last value is zero, a high value point is appended a day after
        // the last.
        throughputByHour.put(
            throughputByHour.lastKey().plus(1, ChronoUnit.DAYS),
            Integer.MAX_VALUE
        );
      }
    }

    /**
     * Finds out the {@link Instant} `end` such that the integral of this {@link ThroughputTrajectory} from `start` to
     * `end` equals `targetBacklog`.
     *
     * @param start         the lower limit of the integral.
     * @param targetBacklog the result of the integral of this trajectory from start to end, measured in `unit x hour`.
     */
    public Instant reversedIntegral(final Instant start, final long targetBacklog) {
      if (targetBacklog == 0) {
        return start;
      } else {
        final var throughputAtStart = throughputByHour.floorEntry(start).getValue();
        final Instant nextInflectionPoint = start.plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
        final long integralUntilNextInflectionPoint =
            (throughputAtStart * ChronoUnit.SECONDS.between(start, nextInflectionPoint)) / SECOND_IN_HOUR;

        if (integralUntilNextInflectionPoint >= targetBacklog) {
          return start.plusSeconds((targetBacklog * SECOND_IN_HOUR) / throughputAtStart);
        } else {
          return reversedIntegral(nextInflectionPoint, targetBacklog - integralUntilNextInflectionPoint);
        }
      }
    }
  }
}
