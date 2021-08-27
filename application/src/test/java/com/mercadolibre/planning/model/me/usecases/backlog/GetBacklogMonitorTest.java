package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.BacklogByDate;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchEntitiesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;
import com.mercadolibre.planning.model.me.utils.TestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBacklogMonitorTest {
    private static final List<ZonedDateTime> DATES = of(
            parse("2021-08-12T01:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T02:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T03:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T04:00:00Z", ISO_OFFSET_DATE_TIME)
    );

    private static final ZonedDateTime DATE_FROM = DATES.get(0);

    private static final ZonedDateTime DATE_TO = DATES.get(3);

    private static final String OUTBOUND_ORDERS = "outbound-orders";


    @InjectMocks
    private GetBacklogMonitor getBacklogMonitor;

    @Mock
    private BacklogApiGateway backlogApiGateway;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private ProjectBacklog backlogProjection;

    @Test
    void testExecuteOK() {
        // GIVEN
        mockBacklogApiResponse();
        mockHistoricalBacklog();
        mockProjectedBacklog();
        mockProductivitySearch();

        // WHEN
        final WorkflowBacklogDetail orders = getBacklogMonitor.execute(input());

        // THEN
        assertNotNull(orders);
        assertEquals("outbound-orders", orders.getWorkflow());

        // waving
        final ProcessDetail waving = orders.getProcesses().get(0);

        assertWavingBacklogResults(waving);
        assertEquals(169000, waving.getBacklogs().get(0).getHistorical());
        assertEquals(172000, waving.getBacklogs().get(3).getHistorical());
    }

    @Test
    void testGetCurrentBacklogError() {
        // GIVEN
        when(backlogApiGateway.getBacklog(any(BacklogRequest.class)))
                .thenThrow(new TestException());

        // WHEN
        assertThrows(
                TestException.class,
                () -> getBacklogMonitor.execute(input())
        );
    }

    @Test
    void testGetProjectedBacklogError() {
        // GIVEN
        mockBacklogApiResponse();
        when(backlogProjection.execute(any(BacklogProjectionInput.class)))
                .thenThrow(new TestException());

        // WHEN
        assertThrows(
                TestException.class,
                () -> getBacklogMonitor.execute(input())
        );
    }

    @Test
    void testGetHistoricalBacklogError() {
        // GIVEN
        mockBacklogApiResponse();
        mockProjectedBacklog();
        mockProductivitySearch();

        final BacklogRequest request = BacklogRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflows(of("outbound-orders"))
                .processes(of("waving", "picking", "packing"))
                .groupingFields(of("workflow", "process", "date_out"))
                .dateFrom(DATE_FROM.minusWeeks(3L))
                .dateTo(DATE_FROM)
                .build();

        when(backlogApiGateway.getBacklog(request)).thenThrow(RuntimeException.class);

        // WHEN
        final WorkflowBacklogDetail orders = getBacklogMonitor.execute(input());

        // THEN
        assertNotNull(orders);
        assertEquals("outbound-orders", orders.getWorkflow());

        // waving
        final ProcessDetail waving = orders.getProcesses().get(0);

        assertWavingBacklogResults(waving);
        assertEquals(0, waving.getBacklogs().get(0).getHistorical());
        assertEquals(0, waving.getBacklogs().get(3).getHistorical());
    }

    @Test
    void testGetProductivityError() {
        // GIVEN
        mockBacklogApiResponse();
        mockHistoricalBacklog();
        mockProjectedBacklog();

        final SearchEntitiesRequest request = SearchEntitiesRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .entityTypes(List.of(THROUGHPUT))
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .processName(List.of(WAVING, PICKING, PACKING))
                .build();

        when(planningModelGateway.searchEntities(request))
                .thenThrow(TestException.class);

        // WHEN
        assertThrows(
                TestException.class,
                () -> getBacklogMonitor.execute(input())
        );
    }

    private void assertWavingBacklogResults(ProcessDetail waving) {
        // waving
        assertEquals("waving", waving.getProcess());
        assertEquals(150, waving.getTotal().getUnits());
        assertEquals(10, waving.getTotal().getMinutes());
        assertEquals(4, waving.getBacklogs().size());

        // past backlog
        final BacklogByDate wavingPastBacklog = waving.getBacklogs().get(0);
        assertEquals(DATE_FROM, wavingPastBacklog.getDate());
        assertEquals(100, wavingPastBacklog.getCurrent().getUnits());
        assertEquals(10, wavingPastBacklog.getCurrent().getMinutes());

        // projected backlog
        final BacklogByDate wavingProjectedBacklog = waving.getBacklogs().get(3);
        assertEquals(DATES.get(3), wavingProjectedBacklog.getDate());
        assertEquals(250, wavingProjectedBacklog.getCurrent().getUnits());
        assertEquals(12, wavingProjectedBacklog.getCurrent().getMinutes());
    }

    private GetBacklogMonitorInputDto input() {
        return new GetBacklogMonitorInputDto(
                WAREHOUSE_ID, OUTBOUND_ORDERS, DATE_FROM, DATE_TO, 0L);
    }

    private void mockBacklogApiResponse() {
        when(backlogApiGateway.getBacklog(any(BacklogRequest.class)))
                .thenReturn(of(
                        new Backlog(DATE_FROM, Map.of(
                                "workflow", "outbound-orders",
                                "process", "waving",
                                "date_out", "2021-08-12T01:00:00Z"
                        ), 100),
                        new Backlog(DATE_FROM, Map.of(
                                "workflow", "outbound-orders",
                                "process", "waving",
                                "date_out", "2021-08-12T02:00:00Z"
                        ), 150),
                        new Backlog(DATE_FROM, Map.of(
                                "workflow", "outbound-orders",
                                "process", "picking",
                                "date_out", "2021-08-12T01:00:00Z"
                        ), 300),
                        new Backlog(DATE_FROM, Map.of(
                                "workflow", "outbound-orders",
                                "process", "picking",
                                "date_out", "2021-08-12T02:00:00Z"
                        ), 350),
                        new Backlog(DATE_FROM, Map.of(
                                "workflow", "outbound-orders",
                                "process", "packing",
                                "date_out", "2021-08-12T01:00:00Z"
                        ), 6000),
                        new Backlog(DATE_FROM, Map.of(
                                "workflow", "outbound-orders",
                                "process", "packing",
                                "date_out", "2021-08-12T02:00:00Z"
                        ), 8000)
                ));
    }

    private void mockHistoricalBacklog() {
        final BacklogRequest request = BacklogRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflows(of("outbound-orders"))
                .processes(of("waving", "picking", "packing"))
                .groupingFields(of("workflow", "process", "date_out"))
                .dateFrom(DATE_FROM.minusWeeks(3L))
                .dateTo(DATE_FROM)
                .build();

        when(backlogApiGateway.getBacklog(request)).thenReturn(
                generateHistoricalBacklog(DATE_FROM.minusWeeks(3L), DATE_FROM)
        );
    }

    private List<Backlog> generateHistoricalBacklog(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        final int hours = (int) ChronoUnit.HOURS.between(dateFrom, dateTo);
        final List<String> processes = of("waving", "packing", "picking");

        return Stream.of(0, 1, 2)
                .map(i -> IntStream.range(0, hours)
                        .mapToObj(h -> new Backlog(DATE_FROM, Map.of(
                                "workflow", "outbound-orders",
                                "process", processes.get(i),
                                "date_out", dateFrom.plusHours(h).format(ISO_OFFSET_DATE_TIME)
                        ), (h + 1) * 1000 * (i + 1)))
                ).collect(Collectors.flatMapping(
                        Function.identity(),
                        Collectors.toList()
                ));
    }

    private void mockProjectedBacklog() {
        when(backlogProjection.execute(any(BacklogProjectionInput.class)))
                .thenReturn(new ProjectedBacklog(
                        of(
                                new BacklogProjectionResponse(
                                        WAVING,
                                        of(
                                                new ProjectionValue(DATES.get(2), 125),
                                                new ProjectionValue(DATES.get(3), 250)
                                        )
                                ),
                                new BacklogProjectionResponse(
                                        PICKING,
                                        of(
                                                new ProjectionValue(DATES.get(2), 410),
                                                new ProjectionValue(DATES.get(3), 630)
                                        )
                                ),
                                new BacklogProjectionResponse(
                                        PACKING,
                                        of(
                                                new ProjectionValue(DATES.get(2), 888),
                                                new ProjectionValue(DATES.get(3), 999)
                                        )
                                )
                        )
                ));
    }

    private void mockProductivitySearch() {
        final SearchEntitiesRequest request = SearchEntitiesRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .entityTypes(List.of(THROUGHPUT))
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .processName(List.of(WAVING, PICKING, PACKING))
                .build();

        when(planningModelGateway.searchEntities(request))
                .thenReturn(Map.of(
                        THROUGHPUT, of(
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(WAVING)
                                        .date(DATES.get(0))
                                        .value(10)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(WAVING)
                                        .date(DATES.get(1))
                                        .value(15)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(WAVING)
                                        .date(DATES.get(2))
                                        .value(20)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(WAVING)
                                        .date(DATES.get(3))
                                        .value(20)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(PICKING)
                                        .date(DATES.get(0))
                                        .value(3)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(PICKING)
                                        .date(DATES.get(1))
                                        .value(1)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(PICKING)
                                        .date(DATES.get(2))
                                        .value(4)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(PICKING)
                                        .date(DATES.get(3))
                                        .value(5)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(PACKING)
                                        .date(DATES.get(0))
                                        .value(1000)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(PACKING)
                                        .date(DATES.get(1))
                                        .value(50)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(PACKING)
                                        .date(DATES.get(2))
                                        .value(20)
                                        .build(),
                                Entity.builder()
                                        .workflow(FBM_WMS_OUTBOUND)
                                        .processName(PACKING)
                                        .date(DATES.get(3))
                                        .value(300)
                                        .build()
                        )
                ));
    }
}
