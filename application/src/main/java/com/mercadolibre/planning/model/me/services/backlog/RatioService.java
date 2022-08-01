package com.mercadolibre.planning.model.me.services.backlog;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator.PackingRatio;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator.RatioInputGroup;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator.RatioWeightsSource;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator.UnitsPerDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * RatioService provides the methods to retrieve backlog ratio metrics.
 */
@Named
@AllArgsConstructor
public class RatioService {

  private static final int GROUPING_KEY_DAYS_MULTIPLIER = 100;

  private static final int DEFAULT_WEEKS = 3;

  private static final Map<ProcessName, Predicate<Photo.Group>> FILTER_BY_PROCESS = Map.of(
      PACKING, RatioService::isPackingTote,
      BATCH_SORTER, RatioService::isPackingWall
  );

  private BacklogApiGateway backlogGateway;

  private static boolean isPackingWall(final Photo.Group group) {
    return group.getGroupValue(STEP)
        .map(Step::valueOf)
        .map(Step.TO_GROUP::equals)
        .orElse(false);
  }

  private static boolean isPackingTote(final Photo.Group group) {
    final var isPacking = group.getGroupValue(STEP)
        .map(Step::valueOf)
        .map(Step.TO_PACK::equals)
        .orElse(false);

    final var isPackingWall = group.getGroupValue(AREA)
        .map("PW"::equals)
        .orElse(false);

    return isPacking && !isPackingWall;
  }

  private static int groupingKey(final ProcessedUnitsAtHourAndProcess photo) {
    final var zonedDate = ZonedDateTime.ofInstant(photo.getDate(), ZoneOffset.UTC);
    return groupingKey(zonedDate);
  }

  private static int groupingKey(final ZonedDateTime zonedDate) {
    return zonedDate.getDayOfWeek().getValue() * GROUPING_KEY_DAYS_MULTIPLIER + zonedDate.getHour();
  }

  /**
   * Calculates the ratios for splitting the picking backlog between packing and consolidation processes.
   *
   * <p>The backlog will be divided based on the logistic center's previous weeks behaviour with respect to packing/consolidation processes.
   *
   * <p>How? The algorithm is encapsulated on @link{PackingRatioCalculator}, this class only responsibility is to gather and build the
   * inputs for that algorithm. The algorithm has two inputs:
   * - Weights Source: How much relevant is a data point based on its date.
   * - Backlog (represented by RatioInputGroup): How many units where processed by each process in the past (DEFAULT_WEEKS) weeks,
   * grouped by target date (all data points relevant for a date will be encapsulated in the same RatioInputGroup).
   *
   * <p>The `Weights Source` is implemented by the static class `ConstantStepRatioWeight` but, if needed, it can be replaced.
   * The `Backlog` is obtained form the backlog-gateway already grouped by process.
   *
   * <p>Implementation: All target hours are shift one hour as backlog-gateway returns the backlog at the start of the hour and this
   * class uses the backlog at the end of it.
   *
   * @param logisticCenterId warehouse id.
   * @param dateFrom         starting date for which the ratio is desired.
   * @param dateTo           ending date for which the ratio is desired.
   * @return A list of ratio per day for both packing and consolidation flow bifurcation.
   */
  public Map<Instant, PackingRatio> getPackingRatio(final String logisticCenterId, final Instant dateFrom, final Instant dateTo) {
    final Map<Instant, RatioInputGroup> inputs = getInputFromPastWeeks(logisticCenterId, dateFrom, dateTo, DEFAULT_WEEKS);
    final RatioWeightsSource weights = new ConstantStepRatioWeight(dateFrom, DEFAULT_WEEKS);

    return PackingRatioCalculator.calculate(inputs, weights);
  }

  private Map<Instant, RatioInputGroup> getInputFromPastWeeks(final String logisticCenterId,
                                                              final Instant dateFrom,
                                                              final Instant dateTo,
                                                              final int weeks) {

    final var zonedDateFrom = ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC);
    final var zonedDateTo = ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC);

    final var groupedPhotos = LongStream.rangeClosed(1, weeks)
        .mapToObj(i -> backlog(
            logisticCenterId,
            zonedDateFrom.minusWeeks(i).toInstant(),
            zonedDateTo.minusWeeks(i).plusHours(1L).toInstant()
        ))
        .flatMap(List::stream)
        .collect(Collectors.groupingBy(RatioService::groupingKey));

    return LongStream.range(0, ChronoUnit.HOURS.between(zonedDateFrom, zonedDateTo) + 1)
        .mapToObj(zonedDateFrom::plusHours)
        .collect(Collectors.toMap(
            ZonedDateTime::toInstant,
            date -> reducePhotos(groupedPhotos.getOrDefault(groupingKey(date), emptyList()))
        ));
  }

  private RatioInputGroup reducePhotos(final List<ProcessedUnitsAtHourAndProcess> photos) {
    final var unitsPerDateByProcess = photos.stream()
        .collect(Collectors.groupingBy(
            ProcessedUnitsAtHourAndProcess::getProcess,
            Collectors.mapping(photo -> new UnitsPerDate(photo.getDate(), photo.getQuantity()), Collectors.toList()))
        );

    return new RatioInputGroup(
        unitsPerDateByProcess.getOrDefault(PACKING, emptyList()),
        unitsPerDateByProcess.getOrDefault(BATCH_SORTER, emptyList())
    );
  }

  // TODO: Update BacklogPhotoApiGateway and move this call to that gateway
  private List<Photo> getBacklog(final String logisticCenterId, final Instant dateFrom, final Instant dateTo) {
    return backlogGateway.getPhotos(
        new BacklogPhotosRequest(
            logisticCenterId,
            Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
            Set.of(Step.TO_GROUP, Step.TO_PACK),
            null,
            null,
            dateFrom,
            dateTo,
            Set.of(STEP, AREA),
            dateFrom,
            dateTo
        )
    );
  }

  private List<ProcessedUnitsAtHourAndProcess> backlog(final String logisticCenterId, final Instant dateFrom, final Instant dateTo) {
    final var photos = getBacklog(logisticCenterId, dateFrom, dateTo);

    return Stream.of(
            calculateProcessedUnitsPerHourForProcess(photos, BATCH_SORTER),
            calculateProcessedUnitsPerHourForProcess(photos, PACKING)
        )
        .flatMap(List::stream)
        .collect(Collectors.toList());

  }

  private List<ProcessedUnitsAtHourAndProcess> calculateProcessedUnitsPerHourForProcess(final List<Photo> photos,
                                                                                        final ProcessName process) {

    final var filter = FILTER_BY_PROCESS.get(process);

    final var packingToteProcessedUnits = photos.stream()
        .map(photo -> photo.getGroups()
            .stream()
            .filter(filter)
            .collect(
                Collectors.collectingAndThen(
                    Collectors.reducing(0, Photo.Group::getAccumulatedTotal, Integer::sum),
                    total -> new ProcessedUnitsAtHourAndProcess(photo.getTakenOn(), process, total))
            )
        )
        .filter(units -> ZonedDateTime.ofInstant(units.getDate(), ZoneOffset.UTC).getMinute() == 0)
        .sorted(Comparator.comparing(ProcessedUnitsAtHourAndProcess::getDate))
        .collect(Collectors.toList());

    return calculateProcessedUnitsPerHour(packingToteProcessedUnits);
  }

  private List<ProcessedUnitsAtHourAndProcess> calculateProcessedUnitsPerHour(
      final List<ProcessedUnitsAtHourAndProcess> totalUnitsProcessed) {

    List<ProcessedUnitsAtHourAndProcess> results = new ArrayList<>();
    for (int i = 0; i < totalUnitsProcessed.size() - 1; i++) {
      final var currentHourTotal = totalUnitsProcessed.get(i);
      final var nextHourTotal = totalUnitsProcessed.get(i + 1);

      results.add(
          new ProcessedUnitsAtHourAndProcess(
              currentHourTotal.getDate(),
              currentHourTotal.getProcess(),
              nextHourTotal.getQuantity() - currentHourTotal.getQuantity()
          )
      );
    }

    return results;
  }

  @Value
  private static class ProcessedUnitsAtHourAndProcess {
    Instant date;

    ProcessName process;

    int quantity;
  }

  static class ConstantStepRatioWeight implements RatioWeightsSource {

    private final Instant dateFrom;

    private final int weeks;

    ConstantStepRatioWeight(final Instant dateFrom, final int weeks) {
      this.dateFrom = dateFrom;
      this.weeks = weeks;
    }


    @Override
    public Double at(final Instant date) {
      final long week = (ChronoUnit.DAYS.between(date.truncatedTo(ChronoUnit.DAYS), dateFrom) - 1) / 7;
      if (week >= weeks) {
        return 0D;
      } else {
        return (double) (weeks - week);
      }
    }
  }

}
