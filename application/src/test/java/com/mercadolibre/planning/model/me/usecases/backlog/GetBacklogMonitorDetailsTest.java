package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBacklogMonitorDetailsTest {
    private static final List<ZonedDateTime> DATES = of(
            parse("2021-08-12T01:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T02:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T03:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T04:00:00Z", ISO_OFFSET_DATE_TIME)
    );

    private static final ZonedDateTime DATE_FROM = DATES.get(0);

    private static final ZonedDateTime DATE_TO = DATES.get(3);

    @InjectMocks
    private GetBacklogMonitorDetails getBacklogMonitor;

    @Mock
    private BacklogApiGateway backlogApiGateway;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private ProjectBacklog backlogProjection;

    @Mock
    private GetProcessThroughput getProcessThroughput;

    @Test
    void testGetBacklogDetails() {
        // GIVEN
        mockPastBacklogWithAreas();
        mockProjectedBacklog();
        mockTargetBacklog();
        mockThroughput(WAVING);

        GetBacklogMonitorDetailsInput input = GetBacklogMonitorDetailsInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow("fbm-wms-outbound")
                .process(WAVING)
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .callerId(999L)
                .build();

        // WHEN
        var response = getBacklogMonitor.execute(input);

        // THEN
        var results = response.getDates();
        assertEquals(4, results.size());

        var firstResult = results.get(0);
        assertEquals(DATE_FROM, firstResult.getDate());

        var firstTotals = firstResult.getTotal();
        assertEquals(90, firstTotals.getUnits());
        assertEquals(9, firstTotals.getMinutes());

        var firstTargets = firstResult.getTarget();
        assertEquals(10, firstTargets.getUnits());
        assertEquals(1, firstTargets.getMinutes());

        var firstAreas = firstResult.getAreas();
        assertEquals(2, firstAreas.size());
        assertEquals("RK-H", firstAreas.get(0).getId());
        assertEquals(15, firstAreas.get(0).getValue().getUnits());
        assertEquals(2, firstAreas.get(0).getValue().getMinutes());

        assertEquals(2, results.get(1).getAreas().size());

        var lastResults = results.get(3);
        assertEquals(DATE_TO, lastResults.getDate());
        assertNotNull(lastResults.getAreas());

        assertEquals(60, lastResults.getTarget().getUnits());
        assertEquals(12, lastResults.getTarget().getMinutes());

        assertEquals(500, lastResults.getTotal().getUnits());
        assertEquals(100, lastResults.getTotal().getMinutes());
    }

    @Test
    void testGetBacklogDetailsWithoutTargets() {
        // GIVEN
        mockPastBacklogWithAreas();
        mockProjectedBacklog();
        mockThroughput(PICKING);

        // WHEN
        GetBacklogMonitorDetailsInput input = GetBacklogMonitorDetailsInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow("fbm-wms-outbound")
                .process(PICKING)
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .callerId(999L)
                .build();

        // WHEN
        var response = getBacklogMonitor.execute(input);

        // THEN
        var results = response.getDates();
        assertEquals(4, results.size());

        var firstResult = results.get(0);
        assertEquals(DATE_FROM, firstResult.getDate());
        assertNull(firstResult.getTarget());

        var firstTotals = firstResult.getTotal();
        assertEquals(90, firstTotals.getUnits());
        assertEquals(9, firstTotals.getMinutes());

        var firstAreas = firstResult.getAreas();
        assertEquals(2, firstAreas.size());
        assertEquals("RK-H", firstAreas.get(0).getId());
        assertEquals(15, firstAreas.get(0).getValue().getUnits());
        assertEquals(2, firstAreas.get(0).getValue().getMinutes());

        var lastResults = results.get(3);
        assertEquals(DATE_TO, lastResults.getDate());
        assertNotNull(lastResults.getAreas());
        assertNull(lastResults.getTarget());
        assertEquals(500, lastResults.getTotal().getUnits());
        assertEquals(100, lastResults.getTotal().getMinutes());

        verify(planningModelGateway, never()).getPerformedProcessing(any());
    }

    @Test
    void testGetBacklogDetailsWithoutAreas() {
        // GIVEN
        mockPastBacklogWithoutAreas();
        mockProjectedBacklog();
        mockTargetBacklog();
        mockThroughput(WAVING);

        // WHEN
        GetBacklogMonitorDetailsInput input = GetBacklogMonitorDetailsInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow("fbm-wms-outbound")
                .process(WAVING)
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .callerId(999L)
                .build();

        // WHEN
        var response = getBacklogMonitor.execute(input);

        // THEN
        var results = response.getDates();
        assertEquals(4, results.size());

        var firstResult = results.get(0);
        assertEquals(DATE_FROM, firstResult.getDate());
        assertNull(firstResult.getAreas());

        var firstTotals = firstResult.getTotal();
        assertEquals(15, firstTotals.getUnits());
        assertEquals(2, firstTotals.getMinutes());

        var firstTargets = firstResult.getTarget();
        assertEquals(10, firstTargets.getUnits());
        assertEquals(1, firstTargets.getMinutes());

        var lastResults = results.get(3);
        assertEquals(DATE_TO, lastResults.getDate());
        assertNull(lastResults.getAreas());

        assertEquals(60, lastResults.getTarget().getUnits());
        assertEquals(12, lastResults.getTarget().getMinutes());

        assertEquals(500, lastResults.getTotal().getUnits());
        assertEquals(100, lastResults.getTotal().getMinutes());
    }

    private void mockPastBacklogWithAreas() {
        ZonedDateTime firstDate = DATES.get(0);
        ZonedDateTime secondDate = DATES.get(1);

        var rkH = Map.of("area", "RK-H");
        var rkL = Map.of("area", "RK-L");

        when(backlogApiGateway.getBacklog(any()))
                .thenReturn(
                        List.of(
                                new Backlog(firstDate, rkH, 15),
                                new Backlog(secondDate, rkH, 50),
                                new Backlog(firstDate, rkL, 75)
                        ));
    }

    private void mockPastBacklogWithoutAreas() {
        var na = Map.of("area", "N/A");
        when(backlogApiGateway.getBacklog(any()))
                .thenReturn(
                        List.of(
                                new Backlog(DATES.get(0), na, 15),
                                new Backlog(DATES.get(1), na, 50)
                        ));
    }

    private void mockProjectedBacklog() {
        when(backlogProjection.execute(any()))
                .thenReturn(
                        new ProjectedBacklog(List.of(
                                new BacklogProjectionResponse(ProcessName.WAVING, of(
                                        new ProjectionValue(DATES.get(2), 300),
                                        new ProjectionValue(DATES.get(3), 500)
                                ))
                        )));
    }

    private void mockTargetBacklog() {
        when(planningModelGateway.getPerformedProcessing(any()))
                .thenReturn(List.of(
                        Entity.builder()
                                .date(DATES.get(0))
                                .value(10)
                                .build(),
                        Entity.builder()
                                .date(DATES.get(1))
                                .value(15)
                                .build(),
                        Entity.builder()
                                .date(DATES.get(2))
                                .value(30)
                                .build(),
                        Entity.builder()
                                .date(DATES.get(3))
                                .value(60)
                                .build()
                ));
    }

    private void mockThroughput(ProcessName process) {
        final GetThroughputInput request = GetThroughputInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processes(List.of(process))
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .build();

        when(getProcessThroughput.execute(request))
                .thenReturn(new GetThroughputResult(
                        Map.of(process, Map.of(
                                DATES.get(0), 10,
                                DATES.get(1), 25,
                                DATES.get(2), 15,
                                DATES.get(3), 5)
                        )
                ));
    }
}