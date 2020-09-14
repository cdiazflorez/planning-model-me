package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class UnitTest {
    @ParameterizedTest
    @CsvSource({
            "1 , MONO",
            "2 , MULTI",
            "10, MULTI"
    })
    public void testCardinality(final int quantity,
                                final UnitGroupCardinality expectedUnitGroupCardinality) {
        // GIVEN
        final Unit unit = dummyUnit(quantity);

        // WHEN
        final UnitGroupCardinality groupCardinality = unit.getGroupCardinality();

        // THEN
        Assertions.assertEquals(expectedUnitGroupCardinality, groupCardinality);
    }

    private Unit dummyUnit(final int quantity) {
        final Unit unit = new Unit();
        final UnitGroup unitGroup = new UnitGroup();
        unitGroup.setQuantity(quantity);
        unit.setGroup(unitGroup);
        return unit;
    }
}

