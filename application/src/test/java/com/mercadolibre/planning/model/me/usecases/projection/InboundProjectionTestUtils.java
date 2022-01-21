package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;

public class InboundProjectionTestUtils {

    public static final DateTimeFormatter TOOLTIP_DATE_FORMATTER = ofPattern("dd/MM - HH:mm");

    public static final DateTimeFormatter DATE_SHORT_FORMATTER = ofPattern("dd/MM HH:mm");

    public static final DateTimeFormatter DATE_ONLY_FORMATTER = ofPattern("dd/MM");

    public static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    public static final TimeZone TIME_ZONE = TimeZone.getTimeZone("America/Argentina/Buenos_Aires");

    private InboundProjectionTestUtils() {
    }

    public static List<Backlog> mockBacklog(final ZonedDateTime currentTime) {
        return List.of(
                new Backlog(currentTime.minusHours(2), -10),
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

    public static SimpleTable mockSimpleTable() {
        return new SimpleTable(
                "title",
                emptyList(),
                emptyList()
        );
    }

}
