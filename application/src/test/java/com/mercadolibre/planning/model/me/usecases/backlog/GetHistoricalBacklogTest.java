package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHistoricalBacklogTest {
    private static final List<ZonedDateTime> DATES = of(
            parse("2021-08-28T01:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-28T02:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-28T03:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-28T04:00:00Z", ISO_OFFSET_DATE_TIME)
    );

    private static final ZonedDateTime REQUEST_DATE = DATES.get(1);

    private static final ZonedDateTime DATE_FROM = DATES.get(0);

    private static final ZonedDateTime DATE_TO = DATES.get(3);


    @InjectMocks
    private GetHistoricalBacklog getHistoricalBacklog;

    @Mock
    private BacklogApiAdapter backlogApiAdapter;

    @Mock
    private GetProcessThroughput getProcessThroughput;

    @Test
    void testExecuteOK() {
        // GIVEN
        final var dateFrom = DATE_FROM.minusWeeks(3L);
        final var dateTo = DATE_TO.minusWeeks(1L).plusHours(1L);

        mockHistoricalBacklog(1);
        mockHistoricalBacklog(2);
        mockHistoricalBacklog(3);

        mockThroughput(dateFrom, dateTo);

        final var input = new GetHistoricalBacklogInput(
                REQUEST_DATE.toInstant(),
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND,
                of(WAVING, PICKING, PACKING),
                DATE_FROM.toInstant(),
                DATE_TO.toInstant()
        );

        // WHEN
        final Map<ProcessName, HistoricalBacklog> backlogs = getHistoricalBacklog.execute(input);

        // THEN
        assertNotNull(backlogs);

        // waving
        final var waving = backlogs.get(WAVING);
        assertEquals(232, waving.get(DATE_FROM.toInstant()).getUnits()); // 239, 232, 225
        assertEquals(181, waving.get(DATE_FROM.toInstant()).getMinutes()); // 77, 79, 82

        assertEquals(910, waving.get(DATE_TO.toInstant()).getUnits()); // 938, 910, 882
        assertEquals(181, waving.get(DATE_TO.toInstant()).getMinutes()); // 303, 312, 322
    }

    private void mockThroughput(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        final List<ZonedDateTime> dates = dates(DATE_FROM, DATE_TO, 3);
        final List<ProcessName> processes = of(WAVING, PICKING, PACKING);

        final Map<ProcessName, Map<ZonedDateTime, Integer>> result = processes.stream()
                .collect(Collectors.toMap(
                        process -> process,
                        process -> dates.stream()
                                .collect(Collectors.toMap(
                                        Function.identity(),
                                        date -> dateValue(date) / (3 - processes.indexOf(process))
                                ))
                ));

        final GetThroughputInput input = GetThroughputInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processes(of(WAVING, PICKING, PACKING))
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();

        when(getProcessThroughput.execute(input)).thenReturn(new GetThroughputResult(result));
    }

    private void mockHistoricalBacklog(final long shift) {
        final List<String> processes = of("waving", "packing", "picking");

        final Instant mockFromDate = DATE_FROM.minusWeeks(shift).toInstant();
        final Instant mockToDate = DATE_TO.minusWeeks(shift).toInstant();

        final long hours = ChronoUnit.HOURS.between(mockFromDate, mockToDate);

        final List<Consolidation> consolidations = processes.stream()
                .flatMap(process -> LongStream.rangeClosed(0, hours)
                        .mapToObj(hour -> mockFromDate.plus(hour, ChronoUnit.HOURS))
                        .map(date -> new Consolidation(
                                date,
                                Map.of("process", process),
                                dateValue(date) * (processes.indexOf(process) + 1),
                                true

                        ))
                )
                .collect(Collectors.toList());

        when(backlogApiAdapter.getCurrentBacklog(
                REQUEST_DATE.toInstant(),
                WAREHOUSE_ID,
                of(FBM_WMS_OUTBOUND),
                of(WAVING, PICKING, PACKING),
                of(PROCESS),
                mockFromDate,
                mockToDate.plus(5, ChronoUnit.MINUTES),
                REQUEST_DATE.minusWeeks(shift).toInstant(),
                REQUEST_DATE.minusWeeks(shift).plusHours(24).toInstant())
        ).thenReturn(consolidations);
    }

    private List<ZonedDateTime> dates(final ZonedDateTime dateFrom,
                                      final ZonedDateTime dateTo,
                                      final int weeks) {

        final long hours = ChronoUnit.HOURS.between(dateFrom, dateTo);

        final List<ZonedDateTime> dates = new ArrayList<>();
        for (int weekShift = 0; weekShift < weeks; weekShift++) {
            var from = dateFrom.minusWeeks(weekShift);
            for (int hoursShift = 0; hoursShift <= hours; hoursShift++) {
                dates.add(from.plusHours(hoursShift));
            }
        }

        return dates;
    }

    private int dateValue(final ZonedDateTime date) {
        return date.getDayOfWeek().getValue() + date.getHour() * date.getDayOfYear();
    }

    private int dateValue(final Instant instant) {
        return dateValue(ZonedDateTime.ofInstant(instant, UTC));
    }
}
