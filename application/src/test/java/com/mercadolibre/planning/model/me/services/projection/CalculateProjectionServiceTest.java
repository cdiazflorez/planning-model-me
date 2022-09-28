package com.mercadolibre.planning.model.me.services.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CalculateProjectionServiceTest {

  private static final Instant CURRENT_DATE = Instant.parse("2022-08-21T21:00:00Z");

  private static final Instant FIRST_OPERATION_DATE = CURRENT_DATE.plus(1, ChronoUnit.HOURS);

  private static final Instant SECOND_OPERATION_DATE = FIRST_OPERATION_DATE.plus(1, ChronoUnit.HOURS);

  private static final Instant THIRD_OPERATION_DATE = SECOND_OPERATION_DATE.plus(1, ChronoUnit.HOURS);

  private static final Instant FIRST_DATE_CPT = Instant.parse("2022-08-22T00:00:00Z");

  private static final Instant SECOND_DATE_CPT = FIRST_DATE_CPT.plus(1, ChronoUnit.HOURS);

  private static final Instant THIRD_DATE_CPT = SECOND_DATE_CPT.plus(2, ChronoUnit.HOURS);

  private static final Instant DATE_TO = THIRD_DATE_CPT.plus(3, ChronoUnit.HOURS);

  private static final ZoneId UTC = ZoneId.of("UTC");

  @InjectMocks
  private CalculateProjectionService calculateProjectionService;

  private static Photo.Group photoGroup(final Step step, final Instant dateOut, final int total) {
    return new Photo.Group(
        Map.of(
            BacklogGrouper.STEP, step.getName(),
            BacklogGrouper.DATE_OUT, dateOut.toString()
        ),
        total,
        0
    );
  }

  private static PlanningDistributionResponse planningDistribution(final Instant dateIn, final Instant dateOut, final int total) {
    return PlanningDistributionResponse.builder()
        .dateIn(dateIn.atZone(UTC))
        .dateOut(dateOut.atZone(UTC))
        .metricUnit(MetricUnit.UNITS)
        .total(total)
        .build();
  }

  private static ProjectionResult projectionResult(final Instant cpt, final Instant projectedEndDate, final Integer rQuantity) {
    return new ProjectionResult(
        ZonedDateTime.ofInstant(cpt, UTC),
        projectedEndDate == null ? null : ZonedDateTime.ofInstant(projectedEndDate, ZoneOffset.UTC),
        null,
        rQuantity,
        new ProcessingTime(10, ChronoUnit.MINUTES.toString()),
        false,
        false,
        null,
        0,
        null
    );
  }

  @Test
  void calculateProjectionServiceTest() {
    // WHEN
    final List<ProjectionResult> projectionResults = calculateProjectionService
        .execute(
            CURRENT_DATE,
            CURRENT_DATE,
            DATE_TO,
            FBM_WMS_OUTBOUND,
            throughputByProcess(),
            currentBacklog(),
            forecastSales(),
            processingTimeByDateOut(),
            ratioByHour()
        );

    // THEN
    List<ProjectionResult> sortedResults = projectionResults.stream()
        .sorted(Comparator.comparing(ProjectionResult::getDate))
        .collect(Collectors.toList());

    assertEquals(expectedProjectionResult(), sortedResults);
  }

  private Map<Instant, PackingRatioCalculator.PackingRatio> ratioByHour() {
    return Map.of(
        FIRST_OPERATION_DATE, new PackingRatioCalculator.PackingRatio(0.5, 0.5),
        SECOND_OPERATION_DATE, new PackingRatioCalculator.PackingRatio(0.5, 0.5)
    );
  }

  private Map<Instant, Integer> fill(final List<Instant> ips, final int value) {
    return ips.stream()
        .collect(Collectors.toMap(
            Function.identity(),
            ip -> value
        ));
  }

  private Map<ProcessName, Map<Instant, Integer>> throughputByProcess() {
    final var hours = ChronoUnit.HOURS.between(CURRENT_DATE, DATE_TO);
    final var ips = LongStream.rangeClosed(0, hours)
        .mapToObj(i -> CURRENT_DATE.plus(i, ChronoUnit.HOURS))
        .collect(Collectors.toList());

    return Map.of(
        PICKING, fill(ips, 600),
        BATCH_SORTER, fill(ips, 600),
        WALL_IN, fill(ips, 600),
        PACKING_WALL, fill(ips, 400),
        PACKING, fill(ips, 400)
    );
  }

  private Map<Instant, ProcessingTime> processingTimeByDateOut() {
    return Map.of(
        FIRST_DATE_CPT, new ProcessingTime(10, ChronoUnit.MINUTES.toString()),
        SECOND_DATE_CPT, new ProcessingTime(10, ChronoUnit.MINUTES.toString()),
        THIRD_DATE_CPT, new ProcessingTime(10, ChronoUnit.MINUTES.toString())
    );
  }

  private List<Photo.Group> currentBacklog() {
    return List.of(
        photoGroup(Step.PENDING, FIRST_DATE_CPT, 700),
        photoGroup(Step.PENDING, SECOND_DATE_CPT, 600),
        photoGroup(Step.PENDING, THIRD_DATE_CPT, 500),
        photoGroup(Step.TO_PICK, FIRST_DATE_CPT, 250),
        photoGroup(Step.TO_PICK, SECOND_DATE_CPT, 450),
        photoGroup(Step.TO_PICK, THIRD_DATE_CPT, 300),
        photoGroup(Step.TO_SORT, FIRST_DATE_CPT, 300),
        photoGroup(Step.TO_SORT, SECOND_DATE_CPT, 200),
        photoGroup(Step.TO_SORT, THIRD_DATE_CPT, 150),
        photoGroup(Step.TO_GROUP, FIRST_DATE_CPT, 400),
        photoGroup(Step.TO_GROUP, SECOND_DATE_CPT, 300),
        photoGroup(Step.TO_GROUP, THIRD_DATE_CPT, 350),
        photoGroup(Step.TO_PACK, FIRST_DATE_CPT, 300),
        photoGroup(Step.TO_PACK, SECOND_DATE_CPT, 200),
        photoGroup(Step.TO_PACK, THIRD_DATE_CPT, 150)
    );
  }

  private List<PlanningDistributionResponse> forecastSales() {
    return List.of(
        planningDistribution(FIRST_OPERATION_DATE, FIRST_DATE_CPT, 200),
        planningDistribution(FIRST_OPERATION_DATE, SECOND_DATE_CPT, 450),
        planningDistribution(FIRST_OPERATION_DATE, THIRD_DATE_CPT, 500),
        planningDistribution(SECOND_OPERATION_DATE, FIRST_DATE_CPT, 300),
        planningDistribution(SECOND_OPERATION_DATE, SECOND_DATE_CPT, 300),
        planningDistribution(SECOND_OPERATION_DATE, THIRD_DATE_CPT, 200),
        planningDistribution(THIRD_OPERATION_DATE, FIRST_DATE_CPT, 1),
        planningDistribution(THIRD_OPERATION_DATE, SECOND_DATE_CPT, 1),
        planningDistribution(THIRD_OPERATION_DATE, THIRD_DATE_CPT, 1)
    );
  }

  private List<ProjectionResult> expectedProjectionResult() {
    return List.of(
        projectionResult(FIRST_DATE_CPT, Instant.parse("2022-08-22T00:41:12Z"), 297),
        projectionResult(SECOND_DATE_CPT, Instant.parse("2022-08-22T04:13:47Z"), 1825),
        projectionResult(THIRD_DATE_CPT, null, 1888)
    );
  }
}
