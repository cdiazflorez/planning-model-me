package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionDataInput;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ProjectionDataMapperTest {


  static final Long FORECAST_UNIT = 0L;

  static final int CURRENT_BACKLOG = 21;

  static final int PROCESSING_TIME = 30;

  static final Double FORECAST_DEVIATION = 0.0;

  static final Instant DATE_TO = Instant.parse("2022-07-13T15:00:00Z");

  static final Instant DATE_FROM = Instant.parse("2022-07-13T09:00:00Z");

  static final List<Map<String, Instant>> DATA_PROJECTION = List.of(
      Map.of(
          "CPT", DATE_FROM,
          "PROJECT_END_DATE", Instant.parse("2022-07-12T16:00:00Z")
      ),
      Map.of(
          "CPT", Instant.parse("2022-07-13T13:00:00Z"),
          "PROJECT_END_DATE", Instant.parse("2022-07-12T14:00:00Z")
      ),
      Map.of(
          "CPT", DATE_TO,
          "PROJECT_END_DATE", Instant.parse("2022-07-12T10:00:00Z")
      )
  );

  @Test
  void testExecuteOkDeferral() {
    // GIVEN
    final GetProjectionDataInput input = input(false);
    final List<Projection> listProjectionMock = mockListProjection(false);

    // WHEN
    final List<Projection> result = ProjectionDataMapper.map(input);

    // THEN
    assertEquals(listProjectionMock, result);
  }

  @Test
  void testExecuteOkCPT() {
    // GIVEN
    final GetProjectionDataInput input = input(true);
    final List<Projection> listProjectionMock = mockListProjection(true);

    // WHEN
    final List<Projection> result = ProjectionDataMapper.map(input);

    // THEN
    assertEquals(listProjectionMock, result);
  }

  private List<ProjectionResult> createProjections() {
    return DATA_PROJECTION.stream()
        .map(elem -> new ProjectionResult(
            elem.get("CPT").atZone(ZoneId.of("UTC")),
            elem.get("PROJECT_END_DATE").atZone(ZoneId.of("UTC")),
            null,
            0,
            new ProcessingTime(PROCESSING_TIME, "minutes"),
            false,
            false,
            null,
            0,
            null
        )).collect(Collectors.toList());
  }

  private List<Backlog> createBacklogs() {
    return DATA_PROJECTION.stream()
        .map(elem -> new Backlog(
            elem.get("CPT").atZone(ZoneId.of("UTC")),
            null,
            CURRENT_BACKLOG
        )).collect(Collectors.toList());
  }

  private List<Projection> mockListProjection(boolean showDeviation) {
    final Long forecastUnit = showDeviation ? FORECAST_UNIT : null;
    final Double forecastDeviation = showDeviation ? FORECAST_DEVIATION : null;

    return Lists.reverse(DATA_PROJECTION.stream()
        .map(elem -> new Projection(
            elem.get("CPT"),
            elem.get("PROJECT_END_DATE"),
            CURRENT_BACKLOG,
            forecastUnit,
            30,
            0,
            false,
            false,
            forecastDeviation,
            null,
            null,
            0,
            null
        )).collect(Collectors.toList()));
  }

  private GetProjectionDataInput input(boolean showDeviation) {
    final ZonedDateTime dateFrom = DATE_FROM.atZone(ZoneId.of("UTC"));

    final ZonedDateTime dateTo = DATE_TO.atZone(ZoneId.of("UTC"));

    final List<ProjectionResult> projections = createProjections();

    final List<Backlog> backlogs = createBacklogs();

    return GetProjectionDataInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .sales(Collections.emptyList())
        .projections(projections)
        .planningDistribution(Collections.emptyList())
        .backlogs(backlogs)
        .showDeviation(showDeviation)
        .build();
  }
}
