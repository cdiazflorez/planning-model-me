package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TimeZone;

public class InboundProjectionTestUtils {

  public static final TimeZone TIME_ZONE = TimeZone.getTimeZone("America/Argentina/Buenos_Aires");

  private InboundProjectionTestUtils() {
  }

  public static List<Backlog> mockBacklog(final ZonedDateTime currentTime) {
    return List.of(
        new Backlog(currentTime.minusHours(1), 150),
        new Backlog(currentTime.plusHours(2), 235),
        new Backlog(currentTime.plusHours(3), 300)
    );
  }

  public static List<Backlog> mockPlanningBacklog(final ZonedDateTime currentTime) {
    final ZonedDateTime truncatedDate = convertToTimeZone(TIME_ZONE.toZoneId(), currentTime.minusHours(1))
        .truncatedTo(ChronoUnit.DAYS);

    final ZonedDateTime overdueSla = convertToTimeZone(ZoneId.of("Z"), truncatedDate);

    return List.of(
        new Backlog(overdueSla, 150),
        new Backlog(currentTime.plusHours(2), 235),
        new Backlog(currentTime.plusHours(3), 300)
    );
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
