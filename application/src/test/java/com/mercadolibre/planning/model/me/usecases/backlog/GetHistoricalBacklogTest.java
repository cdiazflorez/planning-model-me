package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
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
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetHistoricalBacklogTest {
    private static final List<ZonedDateTime> DATES = of(
            parse("2021-08-12T01:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T02:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T03:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T04:00:00Z", ISO_OFFSET_DATE_TIME)
    );

    private static final ZonedDateTime DATE_FROM = DATES.get(0);

    private static final ZonedDateTime DATE_TO = DATES.get(3);


    @InjectMocks
    private GetHistoricalBacklog getHistoricalBacklog;

    @Mock
    private BacklogApiGateway backlogApiGateway;

    @Test
    void testExecuteOK() {
        // GIVEN
        final var dateFrom = DATE_FROM.minusWeeks(3L);
        final var dateTo = DATE_FROM;

        mockHistoricalBacklog(dateFrom, dateTo);

        final var input = GetHistoricalBacklogInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflows(of("outbound-orders"))
                .processes(of(WAVING, PICKING, PACKING))
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();

        // WHEN
        final Map<ProcessName, HistoricalBacklog> backlogs = getHistoricalBacklog.execute(input);

        // THEN
        assertNotNull(backlogs);

        // waving
        final var waving = backlogs.get(WAVING);
        assertEquals(169000, waving.get(DATE_FROM));
        assertEquals(172000, waving.get(DATE_TO));
    }

    private void mockHistoricalBacklog(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        final BacklogRequest request = BacklogRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflows(of("outbound-orders"))
                .processes(of("waving", "picking", "packing"))
                .groupingFields(of("process"))
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();

        when(backlogApiGateway.getBacklog(request)).thenReturn(
                generateHistoricalBacklog(dateFrom, dateTo)
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
}
