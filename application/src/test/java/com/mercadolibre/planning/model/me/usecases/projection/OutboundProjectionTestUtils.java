package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PackingRatio;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

public final class OutboundProjectionTestUtils {

  public static final String BA_ZONE = "America/Argentina/Buenos_Aires";
  public static final TimeZone TIME_ZONE = TimeZone.getTimeZone(BA_ZONE);
  public static final ZonedDateTime UTC_CURRENT_DATE = getCurrentUtcDate();
  public static final ZonedDateTime CPT_1 = UTC_CURRENT_DATE.minusHours(1);
  public static final ZonedDateTime CPT_2 = UTC_CURRENT_DATE.plusHours(2);
  public static final ZonedDateTime CPT_3 = UTC_CURRENT_DATE.plusHours(3);

  private OutboundProjectionTestUtils() {
  }

  public static List<Backlog> mockBacklog(final ZonedDateTime currentTime) {
    return List.of(
        new Backlog(currentTime.minusHours(1), 150),
        new Backlog(currentTime.plusHours(2), 235),
        new Backlog(currentTime.plusHours(3), 300)
    );
  }

  public static Photo generateMockPhoto(final Instant photoDate) {
    return new Photo(
        photoDate,
        List.of(
            new Photo.Group(
                Map.of(DATE_OUT, CPT_1.toString()),
                150,
                0
            ),
            new Photo.Group(
                Map.of(DATE_OUT, CPT_2.toString()),
                235,
                0
            ),
            new Photo.Group(
                Map.of(DATE_OUT, CPT_3.toString()),
                300,
                0
            )
        )
    );
  }

  public static List<ProjectionResult> mockProjectionResults() {
    return List.of(
        new ProjectionResult(
            ZonedDateTime.ofInstant(CPT_1.toInstant(), ZoneId.of("UTC")),
            null,
            null,
            415,
            new ProcessingTime(10, ChronoUnit.MINUTES.toString()),
            true,
            false,
            null,
            0,
            null
        ),
        new ProjectionResult(
            ZonedDateTime.ofInstant(CPT_2.toInstant(), ZoneId.of("UTC")),
            null,
            null,
            500,
            new ProcessingTime(10, ChronoUnit.MINUTES.toString()),
            false,
            false,
            null,
            0,
            null
        ),
        new ProjectionResult(
            ZonedDateTime.ofInstant(CPT_3.toInstant(), ZoneId.of("UTC")),
            null,
            null,
            950,
            new ProcessingTime(10, ChronoUnit.MINUTES.toString()),
            false,
            false,
            null,
            0,
            null
        )
    );
  }

  public static List<PlanningDistributionResponse> mockExpectedBacklog() {
    return List.of(
        new PlanningDistributionResponse(
            CPT_1.minusHours(1),
            CPT_1,
            MetricUnit.UNITS,
            10
        ),
        new PlanningDistributionResponse(
            CPT_2.minusHours(1),
            CPT_2,
            MetricUnit.UNITS,
            30
        ),
        new PlanningDistributionResponse(
            CPT_3.minusHours(1),
            CPT_3,
            MetricUnit.UNITS,
            40
        )
    );
  }

  public static Map<Instant, PackingRatio> generateMockPackingRatioByHour(
      final Instant currentDate,
      final Instant dateTo
  ) {
    Instant date = currentDate.truncatedTo(HOURS);
    final HashMap<Instant, PackingRatio> ratioByHour = new HashMap<>();
    final PackingRatio packingRatio = new PackingRatio(1.0, 0.0);

    while (date.isBefore(dateTo) || date.equals(dateTo)) {
      ratioByHour.put(date, packingRatio);

      date = date.plus(1, HOURS);
    }

    return ratioByHour;
  }

  public static Map<MagnitudeType, List<MagnitudePhoto>> generateMockMagnitudesPhoto(
      final ZonedDateTime currentDate,
      final ZonedDateTime dateTo
  ) {
    ZonedDateTime date = currentDate.truncatedTo(HOURS);
    final List<MagnitudePhoto> magnitudesPhoto = new ArrayList<>();
    final var processNames = List.of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);

    while (date.isBefore(dateTo) || date.equals(dateTo)) {
      ZonedDateTime finalDate = date;
      processNames.forEach(processName -> magnitudesPhoto.add(
          MagnitudePhoto.builder()
              .date(finalDate)
              .value(1000)
              .processName(processName)
              .build()));

      date = date.plusHours(1);
    }

    return Map.of(THROUGHPUT, magnitudesPhoto);
  }

  public static Map<ProcessName, Map<Instant, Integer>> generateMockThroughput(final List<MagnitudePhoto> magnitudes) {
    return magnitudes.stream().collect(Collectors.groupingBy(MagnitudePhoto::getProcessName,
        Collectors.toMap(
            entry -> entry.getDate().toInstant(),
            MagnitudePhoto::getValue)));
  }

  public static ComplexTable mockComplexTable() {
    return new ComplexTable(
        emptyList(),
        emptyList(),
        new ComplexTableAction("applyLabel", "cancelLabel", "editLabel"),
        "title"
    );
  }

}
