package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mercadolibre.planning.model.me.exception.EnumNotFoundException;
import org.junit.jupiter.api.Test;

public class UnitGroupTypeTest {

    @Test
    public void testValidUnitGroupType() {

        // GIVEN
        final UnitGroupType unitGroupType = UnitGroupType.ORDER;

        // WHEN
        final String value = unitGroupType.toJson();

        // THEN
        assertEquals("order", value);
    }

    @Test
    public void testInvalidUnitGroupType() {

        // WHEN
        final EnumNotFoundException exception = assertThrows(EnumNotFoundException.class,
                () -> UnitGroupType.from("INVALID_GROUP_TYPE"));

        // THEN
        assertEquals("Enum not found for value: INVALID_GROUP_TYPE", exception.getMessage());
    }
}
