package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnitStatusTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "PENDING",
            "pending"
    })
    public void testParsing(final String value) {
        // WHEN
        final UnitStatus unitStatus = UnitStatus.from(value);

        // THEN
        assertEquals(UnitStatus.PENDING, unitStatus);
    }

    @Test
    public void testToJson() {
        // GIVEN
        final UnitStatus unitStatus = UnitStatus.TO_GROUP;

        // WHEN
        final String value = unitStatus.toJson();

        // THEN
        assertEquals("to_group", value);
    }
}
