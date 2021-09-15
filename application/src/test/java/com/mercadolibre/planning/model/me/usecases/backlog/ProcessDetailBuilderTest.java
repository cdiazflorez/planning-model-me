package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.ProcessDetailBuilderInput;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProcessDetailBuilderTest {
    private static final ZonedDateTime DATE_A = parse("2021-08-12T01:00:00Z");
    private static final ZonedDateTime DATE_B = parse("2021-08-12T02:00:00Z");
    private static final ZonedDateTime DATE_C = parse("2021-08-12T03:00:00Z");
    private static final ZonedDateTime DATE_D = parse("2021-08-12T04:00:00Z");

    private static ZonedDateTime parse(String s) {
        return ZonedDateTime.parse(s, ISO_OFFSET_DATE_TIME);
    }

    @Test
    void testExecuteOk() {
        // GIVEN
        ProcessDetailBuilder descriptionBuilder = new ProcessDetailBuilder();

        // WHEN
        var result = descriptionBuilder.execute(input());

        // THEN
        assertNotNull(result);
        assertEquals("waving", result.getProcess());

        assertEquals(125, result.getTotal().getUnits());
        assertEquals(8, result.getTotal().getMinutes());
        assertEquals(4, result.getBacklogs().size());

        var firstBacklog = result.getBacklogs().get(0);
        assertEquals(DATE_A, firstBacklog.getDate());
        assertEquals(100, firstBacklog.getCurrent().getUnits());
        assertEquals(10, firstBacklog.getCurrent().getMinutes());
        assertEquals(115, firstBacklog.getHistorical().getUnits());
        assertNull(firstBacklog.getHistorical().getMinutes());
        assertNull(firstBacklog.getMaxLimit());
        assertNull(firstBacklog.getMinLimit());

        var lastBacklog = result.getBacklogs().get(3);
        assertEquals(DATE_D, lastBacklog.getDate());
        assertEquals(200, lastBacklog.getCurrent().getUnits());
        assertEquals(8, lastBacklog.getCurrent().getMinutes());
        assertEquals(185, lastBacklog.getHistorical().getUnits());
        assertNull(lastBacklog.getHistorical().getMinutes());
        assertNull(lastBacklog.getMaxLimit());
        assertNull(lastBacklog.getMinLimit());
    }

    ProcessDetailBuilderInput input() {
        return new ProcessDetailBuilderInput(
                ProcessName.WAVING,
                DATE_B,
                List.of(
                        new BacklogStatsByDate(DATE_A, 100, 10, 115),
                        new BacklogStatsByDate(DATE_B, 125, 15, 100),
                        new BacklogStatsByDate(DATE_C, 150, 30, 130),
                        new BacklogStatsByDate(DATE_D, 200, 23, 185)
                )
        );
    }
}
