package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetProjectionSummaryTest {

    private static final ZonedDateTime CPT_1 = getCurrentUtcDate().plusHours(4);
    private static final ZonedDateTime CPT_2 = getCurrentUtcDate().plusHours(5);
    private static final ZonedDateTime CPT_3 = getCurrentUtcDate().plusHours(5).plusMinutes(30);
    private static final ZonedDateTime CPT_4 = getCurrentUtcDate().plusHours(6);
    private static final ZonedDateTime CPT_5 = getCurrentUtcDate().plusHours(7);

    @InjectMocks
    private GetProjectionSummary getProjectionSummary;

    @Mock
    private GetSales getSales;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Test
    public void textExecute() {
        //GIVEN
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(getDefault()));

        when(getSales.execute(Mockito.any(GetSalesInputDto.class))).thenReturn(mockSales());

        when(planningModelGateway.getPlanningDistribution(any(PlanningDistributionRequest.class)))
                .thenReturn(mockPlanningDistribution(utcCurrentTime));

        //WHEN
        final SimpleTable response = getProjectionSummary.execute(GetProjectionSummaryInput
                .builder()
                .warehouseId("ARTW01")
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .dateFrom(utcCurrentTime)
                .dateTo(utcCurrentTime.plusDays(1))
                .backlogs(mockBacklog())
                .projections(mockProjections(utcCurrentTime))
                .showDeviation(true)
                .build());

        //THEN
        assertProjectionDetailsTable(response);
    }

    private List<ProjectionResult> mockProjections(ZonedDateTime utcCurrentTime) {
        return List.of(
                ProjectionResult.builder()
                        .date(CPT_1)
                        .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(30))
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_2)
                        .projectedEndDate(utcCurrentTime.plusHours(3))
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_3)
                        .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
                        .remainingQuantity(100)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_4)
                        .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
                        .remainingQuantity(180)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_5)
                        .projectedEndDate(null)
                        .remainingQuantity(100)
                        .processingTime(null)
                        .build()
        );
    }

    private List<Backlog> mockBacklog() {
        return List.of(
                new Backlog(CPT_1, 150),
                new Backlog(CPT_2, 235),
                new Backlog(CPT_3, 300),
                new Backlog(CPT_4, 120)
        );
    }

    private List<Backlog> mockSales() {
        return List.of(
                new Backlog(CPT_1, 350),
                new Backlog(CPT_2, 235),
                new Backlog(CPT_3, 200),
                new Backlog(CPT_4, 120)
        );
    }

    private List<PlanningDistributionResponse> mockPlanningDistribution(
            final ZonedDateTime utcCurrentTime) {
        return List.of(
                new PlanningDistributionResponse(utcCurrentTime, CPT_1, MetricUnit.UNITS, 281),
                new PlanningDistributionResponse(utcCurrentTime, CPT_1, MetricUnit.UNITS, 128),
                new PlanningDistributionResponse(utcCurrentTime, CPT_2, MetricUnit.UNITS, 200),
                new PlanningDistributionResponse(utcCurrentTime, CPT_3, MetricUnit.UNITS, 207),
                new PlanningDistributionResponse(utcCurrentTime, CPT_4, MetricUnit.UNITS, 44),
                new PlanningDistributionResponse(utcCurrentTime, CPT_4, MetricUnit.UNITS, 82),
                new PlanningDistributionResponse(utcCurrentTime, CPT_5, MetricUnit.UNITS, 100)
        );
    }

    private void assertProjectionDetailsTable(final SimpleTable projectionDetailsTable) {
        final ZoneId zoneId = getDefault().toZoneId();
        final ZonedDateTime currentTime = convertToTimeZone(zoneId, getCurrentUtcDate());

        assertEquals("Resumen de Proyección", projectionDetailsTable.getTitle());
        assertEquals(4, projectionDetailsTable.getColumns().size());
        assertEquals(6, projectionDetailsTable.getData().size());

        final Map<String, Object> cpt0 = projectionDetailsTable.getData().get(5);
        final Map<String, Object> cpt1 = projectionDetailsTable.getData().get(4);
        final Map<String, Object> cpt2 = projectionDetailsTable.getData().get(3);
        final Map<String, Object> cpt3 = projectionDetailsTable.getData().get(2);
        final Map<String, Object> cpt4 = projectionDetailsTable.getData().get(1);
        final Map<String, Object> cpt5 = projectionDetailsTable.getData().get(0);

        assertEquals("warning", cpt1.get("style"));
        assertEquals(convertToTimeZone(zoneId, CPT_1).format(HOUR_MINUTES_FORMATTER),
                cpt1.get("column_1"));
        assertEquals("150", cpt1.get("column_2"));
        assertEquals("-14.4%", cpt1.get("column_3"));
        assertEquals(currentTime.plusHours(3).plusMinutes(30).format(HOUR_MINUTES_FORMATTER),
                cpt1.get("column_4"));

        assertEquals("warning", cpt2.get("style"));
        assertEquals(convertToTimeZone(zoneId, CPT_2).format(HOUR_MINUTES_FORMATTER),
                cpt2.get("column_1"));
        assertEquals("235", cpt2.get("column_2"));
        assertEquals("17.5%", cpt2.get("column_3"));
        assertEquals(currentTime.plusHours(3).format(HOUR_MINUTES_FORMATTER), cpt2.get("column_4"));

        assertEquals("warning", cpt3.get("style"));
        assertEquals(convertToTimeZone(zoneId, CPT_3).format(HOUR_MINUTES_FORMATTER),
                cpt3.get("column_1"));
        assertEquals("300", cpt3.get("column_2"));
        assertEquals("-3.4%", cpt3.get("column_3"));
        assertEquals(currentTime.plusHours(3).plusMinutes(25).format(HOUR_MINUTES_FORMATTER),
                cpt3.get("column_4"));

        assertEquals("danger", cpt4.get("style"));
        assertEquals(convertToTimeZone(zoneId, CPT_4).format(HOUR_MINUTES_FORMATTER),
                cpt4.get("column_1"));
        assertEquals("120", cpt4.get("column_2"));
        assertEquals("-4.8%", cpt4.get("column_3"));
        assertEquals(currentTime.plusHours(8).plusMinutes(10).format(HOUR_MINUTES_FORMATTER),
                cpt4.get("column_4"));

        assertEquals("danger", cpt5.get("style"));
        assertEquals(convertToTimeZone(zoneId, CPT_5).format(HOUR_MINUTES_FORMATTER),
                cpt5.get("column_1"));
        assertEquals("0", cpt5.get("column_2"));
        assertEquals("0.0%", cpt5.get("column_3"));
        assertEquals("Excede las 24hs", cpt5.get("column_4"));

        assertEquals("none", cpt0.get("style"));
        assertEquals("Total", cpt0.get("column_1"));
        assertEquals("805", cpt0.get("column_2"));
        assertEquals("-13.15%", cpt0.get("column_3"));
        assertEquals("", cpt0.get("column_4"));
    }
}
