package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
  @Mock
  BacklogPhotoApiGateway backlogPhotoApiGateway;
  @InjectMocks
  private GetHistoricalBacklog getHistoricalBacklog;
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
    assertEquals(239, waving.get(DATE_FROM.toInstant()).getUnits()); // 239, 232, 225
    assertEquals(184, waving.get(DATE_FROM.toInstant()).getMinutes()); // 77, 79, 82

    assertEquals(938, waving.get(DATE_TO.toInstant()).getUnits()); // 938, 910, 882
    assertEquals(183, waving.get(DATE_TO.toInstant()).getMinutes()); // 303, 312, 322
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

    final Instant mockFromDate = DATE_FROM.minusWeeks(shift).toInstant();
    final Instant mockToDate = DATE_TO.minusWeeks(shift).toInstant();

    when(backlogPhotoApiGateway.getTotalBacklogPerProcessAndInstantDate(
        new BacklogRequest(
            WAREHOUSE_ID,
            Set.copyOf(of(FBM_WMS_OUTBOUND)),
            Set.copyOf(of(WAVING, PICKING, PACKING)),
            mockFromDate,
            mockToDate.plus(5, ChronoUnit.MINUTES),
            null,
            null,
            REQUEST_DATE.minusWeeks(shift).toInstant(),
            REQUEST_DATE.minusWeeks(shift).plusHours(24).toInstant(),
            Set.copyOf(of(STEP, AREA))),
        true
    )).thenReturn(Map.of(
        WAVING, of(new BacklogPhoto(mockFromDate.plus(0, ChronoUnit.HOURS), 239),
            new BacklogPhoto(mockFromDate.plus(1, ChronoUnit.HOURS), 472),
            new BacklogPhoto(mockFromDate.plus(2, ChronoUnit.HOURS), 705),
            new BacklogPhoto(mockFromDate.plus(3, ChronoUnit.HOURS), 938)),
        PICKING, of(new BacklogPhoto(mockFromDate.plus(0, ChronoUnit.HOURS), 717),
            new BacklogPhoto(mockFromDate.plus(1, ChronoUnit.HOURS), 1416),
            new BacklogPhoto(mockFromDate.plus(2, ChronoUnit.HOURS), 2115),
            new BacklogPhoto(mockFromDate.plus(3, ChronoUnit.HOURS), 2814)),
        PACKING, of(new BacklogPhoto(mockFromDate.plus(0, ChronoUnit.HOURS), 478),
            new BacklogPhoto(mockFromDate.plus(1, ChronoUnit.HOURS), 944),
            new BacklogPhoto(mockFromDate.plus(2, ChronoUnit.HOURS), 1410),
            new BacklogPhoto(mockFromDate.plus(3, ChronoUnit.HOURS), 1876))
    ));

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
