package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.BacklogsByDate;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private ProjectBacklog backlogProjection;

    @Mock
    private GetProcessThroughput getProcessThroughput;

    @Test
    void testExecuteOK() {
        // GIVEN
        mockBacklogApiResponse();
        mockHistoricalBacklog();
        mockProjectedBacklog();
        mockThroughput();

        // WHEN
        final WorkflowBacklogDetail orders = getBacklogMonitor.execute(input());

        // THEN
        assertNotNull(orders);
        assertEquals("outbound-orders", orders.getWorkflow());

        // waving
        final ProcessDetail waving = orders.getProcesses().get(0);

        assertWavingBacklogResults(waving);
        assertEquals(169000, waving.getBacklogs().get(0).getHistorical().getUnits());
        assertEquals(172000, waving.getBacklogs().get(3).getHistorical().getUnits());
        assertNull(waving.getBacklogs().get(0).getHistorical().getMinutes());
        assertNull(waving.getBacklogs().get(3).getHistorical().getMinutes());
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
        mockHistoricalBacklog();
        mockThroughput();

        when(backlogProjection.execute(any(BacklogProjectionInput.class)))
                .thenThrow(new TestException());

        // WHEN
        final WorkflowBacklogDetail orders = getBacklogMonitor.execute(input());

        // THEN
        assertNotNull(orders);
        assertEquals("outbound-orders", orders.getWorkflow());

        // waving
        final ProcessDetail waving = orders.getProcesses().get(0);
        assertEquals(2, waving.getBacklogs().size());
    }

    @Test
    void testGetHistoricalBacklogError() {
        // GIVEN
        mockBacklogApiResponse();
        mockProjectedBacklog();
        mockThroughput();

        final BacklogRequest request = BacklogRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflows(of("outbound-orders"))
                .processes(of("waving", "picking", "packing"))
                .groupingFields(of("process"))
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
        assertEquals(0, waving.getBacklogs().get(0).getHistorical().getUnits());
        assertEquals(0, waving.getBacklogs().get(3).getHistorical().getUnits());
        assertNull(waving.getBacklogs().get(0).getHistorical().getMinutes());
        assertNull(waving.getBacklogs().get(3).getHistorical().getMinutes());
    }

    @Test
    void testGetThroughputError() {
        // GIVEN
        mockBacklogApiResponse();
        mockHistoricalBacklog();
        mockProjectedBacklog();

        final GetThroughputInput request = GetThroughputInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processes(List.of(WAVING, PICKING, PACKING))
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .build();

        when(getProcessThroughput.execute(request))
                .thenThrow(TestException.class);

        // WHEN
        final WorkflowBacklogDetail orders = getBacklogMonitor.execute(input());

        // THEN
        assertNotNull(orders);
        assertEquals("outbound-orders", orders.getWorkflow());

        // waving
        final ProcessDetail waving = orders.getProcesses().get(0);

        assertEquals("waving", waving.getProcess());
        assertEquals(4, waving.getBacklogs().size());
        assertEquals(150, waving.getTotal().getUnits());
        assertEquals(0, waving.getTotal().getMinutes());
    }

    private void assertWavingBacklogResults(ProcessDetail waving) {
        // waving
        assertEquals("waving", waving.getProcess());
        assertEquals(150, waving.getTotal().getUnits());
        assertEquals(10, waving.getTotal().getMinutes());
        assertEquals(4, waving.getBacklogs().size());

        // past backlog
        final BacklogsByDate wavingPastBacklog = waving.getBacklogs().get(0);
        assertEquals(DATE_FROM, wavingPastBacklog.getDate());
        assertEquals(100, wavingPastBacklog.getCurrent().getUnits());
        assertEquals(10, wavingPastBacklog.getCurrent().getMinutes());

        final BacklogsByDate wavingNullMinutesBacklog = waving.getBacklogs().get(2);
        assertEquals(DATES.get(2), wavingNullMinutesBacklog.getDate());
        assertNull(wavingNullMinutesBacklog.getCurrent().getMinutes());

        // projected backlog
        final BacklogsByDate wavingProjectedBacklog = waving.getBacklogs().get(3);
        assertEquals(DATES.get(3), wavingProjectedBacklog.getDate());
        assertEquals(250, wavingProjectedBacklog.getCurrent().getUnits());
        assertEquals(12, wavingProjectedBacklog.getCurrent().getMinutes());
    }

    private GetBacklogMonitorInputDto input() {
        return new GetBacklogMonitorInputDto(
                WAREHOUSE_ID, OUTBOUND_ORDERS, DATE_FROM, DATE_TO, 0L);
    }

    private void mockBacklogApiResponse() {
        ZonedDateTime firstDate = DATES.get(0);
        ZonedDateTime secondDate = DATES.get(1);

        when(backlogApiGateway.getBacklog(any(BacklogRequest.class)))
                .thenReturn(of(
                        new Backlog(firstDate, Map.of(
                                "process", "waving"
                        ), 100),
                        new Backlog(secondDate, Map.of(
                                "process", "waving"
                        ), 150),
                        new Backlog(firstDate, Map.of(
                                "process", "picking"
                        ), 300),
                        new Backlog(secondDate, Map.of(
                                "process", "picking"
                        ), 350),
                        new Backlog(firstDate, Map.of(
                                "process", "packing"
                        ), 6000),
                        new Backlog(secondDate, Map.of(
                                "process", "packing"
                        ), 8000)
                ));
    }

    private void mockHistoricalBacklog() {
        final BacklogRequest request = BacklogRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflows(of("outbound-orders"))
                .processes(of("waving", "picking", "packing"))
                .groupingFields(of("process"))
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
                        .mapToObj(h -> new Backlog(dateFrom.plusHours(h), Map.of(
                                "process", processes.get(i)
                        ), (h + 1) * 1000 * (i + 1)))
                ).collect(Collectors.flatMapping(
                        Function.identity(),
                        Collectors.toList()
                ));
    }

    private void mockProjectedBacklog() {
        ZonedDateTime firstDate = DATES.get(2);
        ZonedDateTime secondDate = DATES.get(3);

        when(backlogProjection.execute(any(BacklogProjectionInput.class)))
                .thenReturn(new ProjectedBacklog(
                        of(
                                new BacklogProjectionResponse(
                                        WAVING,
                                        of(
                                                new ProjectionValue(firstDate, 125),
                                                new ProjectionValue(secondDate, 250)
                                        )
                                ),
                                new BacklogProjectionResponse(
                                        PICKING,
                                        of(
                                                new ProjectionValue(firstDate, 410),
                                                new ProjectionValue(secondDate, 630)
                                        )
                                ),
                                new BacklogProjectionResponse(
                                        PACKING,
                                        of(
                                                new ProjectionValue(firstDate, 888),
                                                new ProjectionValue(secondDate, 999)
                                        )
                                )
                        )
                ));
    }

    private void mockThroughput() {
        final GetThroughputInput request = GetThroughputInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processes(List.of(WAVING, PICKING, PACKING))
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .build();

        when(getProcessThroughput.execute(request))
                .thenReturn(new GetThroughputResult(
                        Map.of(
                                WAVING, Map.of(
                                        DATES.get(0), 10,
                                        DATES.get(1), 15,
                                        DATES.get(2), 0,
                                        DATES.get(3), 20),
                                PICKING, Map.of(
                                        DATES.get(0), 3,
                                        DATES.get(1), 1,
                                        DATES.get(2), 4,
                                        DATES.get(3), 5),
                                PACKING, Map.of(
                                        DATES.get(0), 1000,
                                        DATES.get(1), 50,
                                        DATES.get(2), 20,
                                        DATES.get(3), 300)
                        )
                ));
    }
}
