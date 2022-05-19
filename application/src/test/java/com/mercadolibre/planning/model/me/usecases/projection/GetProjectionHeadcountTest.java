package com.mercadolibre.planning.model.me.usecases.projection;

import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.MetricRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.AreaResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.MetricResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.ProcessResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadCountByArea;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetProjectionHeadcountTest {

  private static final String WH = "ARTW01";

  private static final String METRIC_NAME = "effective_productivity";

  private static final Instant FROM = Instant.parse("2022-05-08T15:00:00Z");

  private static final Instant TO = Instant.parse("2022-05-08T22:00:00Z");

  @InjectMocks
  private GetProjectionHeadcount getProjectionHeadcount;

  @Mock
  private StaffingGateway staffingGateway;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Test
  public void projectionHeadcountTest() {
    //GIVEN
    when(staffingGateway.getMetricsByName(WH, METRIC_NAME,
        new MetricRequest(ProcessName.PICKING, FROM.minus(1, ChronoUnit.HOURS), FROM))
    ).thenReturn(getMetricsByNameMock());

    when(planningModelGateway.getTrajectories(TrajectoriesRequest.builder()
        .warehouseId(WH)
        .dateFrom(ZonedDateTime.ofInstant(FROM, ZoneOffset.UTC))
        .dateTo(ZonedDateTime.ofInstant(TO, ZoneOffset.UTC))
        .source(Source.SIMULATION)
        .processName(List.of(ProcessName.PICKING))
        .processingType(List.of(ProcessingType.ACTIVE_WORKERS))
        .workflow(Workflow.FBM_WMS_OUTBOUND)
        .entityType(MagnitudeType.HEADCOUNT)
        .build())
    ).thenReturn(getTrajectoriesMock());

    //WHEN

    Map<Instant, List<HeadCountByArea>> response = getProjectionHeadcount.getProjectionHeadcount(WH, backlogsMock());

    //THEN
    Assertions.assertNotNull(response);
  }

  private MetricResponse getMetricsByNameMock() {

    List<AreaResponse> areaResponseList = List.of(new AreaResponse("MZ-1", 99.48),
        new AreaResponse("MZ-2", 104.18),
        new AreaResponse("MZ-0", 118.73),
        new AreaResponse("MZ-3", 73.91),
        new AreaResponse("RS", 105.64),
        new AreaResponse("HV", 181.28),
        new AreaResponse("BL", 31.53),
        new AreaResponse("RK-H", 30.32),
        new AreaResponse("RK-L", 17.01));

    ProcessResponse processResponse = new ProcessResponse("picking", 662.6, areaResponseList);

    return new MetricResponse(METRIC_NAME, FROM, TO, List.of(processResponse));

  }

  private List<MagnitudePhoto> getTrajectoriesMock() {

    return List.of(MagnitudePhoto.builder()
            .processName(ProcessName.PICKING)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .date(ZonedDateTime.of(2022, 5, 8, 15, 0, 0, 0, ZoneOffset.UTC))
            .value(80)
            .metricUnit(MetricUnit.WORKERS).build(),
        MagnitudePhoto.builder()
            .processName(ProcessName.PICKING)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .date(ZonedDateTime.of(2022, 5, 8, 16, 0, 0, 0, ZoneOffset.UTC))
            .value(48)
            .metricUnit(MetricUnit.WORKERS).build(),
        MagnitudePhoto.builder()
            .processName(ProcessName.PICKING)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .date(ZonedDateTime.of(2022, 5, 8, 17, 0, 0, 0, ZoneOffset.UTC))
            .value(50)
            .metricUnit(MetricUnit.WORKERS).build(),
        MagnitudePhoto.builder()
            .processName(ProcessName.PICKING)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .date(ZonedDateTime.of(2022, 5, 8, 18, 0, 0, 0, ZoneOffset.UTC))
            .value(56)
            .metricUnit(MetricUnit.WORKERS).build(),
        MagnitudePhoto.builder()
            .processName(ProcessName.PICKING)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .date(ZonedDateTime.of(2022, 5, 8, 19, 0, 0, 0, ZoneOffset.UTC))
            .value(60)
            .metricUnit(MetricUnit.WORKERS).build(),
        MagnitudePhoto.builder()
            .processName(ProcessName.PICKING)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .date(ZonedDateTime.of(2022, 5, 8, 20, 0, 0, 0, ZoneOffset.UTC))
            .value(56)
            .metricUnit(MetricUnit.WORKERS).build(),
        MagnitudePhoto.builder()
            .processName(ProcessName.PICKING)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .date(ZonedDateTime.of(2022, 5, 8, 21, 0, 0, 0, ZoneOffset.UTC))
            .value(40)
            .metricUnit(MetricUnit.WORKERS).build(),
        MagnitudePhoto.builder()
            .processName(ProcessName.PICKING)
            .workflow(Workflow.FBM_WMS_OUTBOUND)
            .date(ZonedDateTime.of(2022, 5, 8, 22, 0, 0, 0, ZoneOffset.UTC))
            .value(30)
            .metricUnit(MetricUnit.WORKERS).build()
    );
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> backlogsMock() {
    Map<Instant, List<NumberOfUnitsInAnArea>> response = new HashMap<>();
    response.put(Instant.parse("2022-05-08T15:00:00Z"), numberOfUnitsInAnAreas());
    response.put(Instant.parse("2022-05-08T16:00:00Z"), numberOfUnitsInAnAreas());
    response.put(Instant.parse("2022-05-08T17:00:00Z"), numberOfUnitsInAnAreas());
    response.put(Instant.parse("2022-05-08T18:00:00Z"), numberOfUnitsInAnAreas());
    response.put(Instant.parse("2022-05-08T19:00:00Z"), numberOfUnitsInAnAreas());
    response.put(Instant.parse("2022-05-08T20:00:00Z"), numberOfUnitsInAnAreas());
    response.put(Instant.parse("2022-05-08T21:00:00Z"), numberOfUnitsInAnAreas());
    response.put(Instant.parse("2022-05-08T22:00:00Z"), numberOfUnitsInAnAreas());

    return response;
  }

  private List<NumberOfUnitsInAnArea> numberOfUnitsInAnAreas() {
    return List.of(new NumberOfUnitsInAnArea("MZ",
            List.of(new NumberOfUnitsInAnArea.NumberOfUnitsInASubarea("MZ-1", 100),
                new NumberOfUnitsInAnArea.NumberOfUnitsInASubarea("MZ-2", 150),
                new NumberOfUnitsInAnArea.NumberOfUnitsInASubarea("MZ-0", 50),
                new NumberOfUnitsInAnArea.NumberOfUnitsInASubarea("MZ-3", 80)
            )),
        new NumberOfUnitsInAnArea("RS", List.of(new NumberOfUnitsInAnArea.NumberOfUnitsInASubarea("RS-0", 70))),
        new NumberOfUnitsInAnArea("HV", List.of(new NumberOfUnitsInAnArea.NumberOfUnitsInASubarea("HV-0", 200))),
        new NumberOfUnitsInAnArea("BL", List.of(new NumberOfUnitsInAnArea.NumberOfUnitsInASubarea("BL-0", 120))),
        new NumberOfUnitsInAnArea("RK",
            List.of(new NumberOfUnitsInAnArea.NumberOfUnitsInASubarea("RK-H", 250),
                new NumberOfUnitsInAnArea.NumberOfUnitsInASubarea("RK-L", 300)))
    );
  }
}
