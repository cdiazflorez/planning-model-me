package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Value;

@Value
public class UnitMeasure {
    private static final double MINUTES_IN_HOUR = 60.0;

    private Integer units;
    private Integer minutes;

    public static UnitMeasure from(Integer units, Integer throughput) {
        final Integer finalUnits = units >= 0 ? units : 0;
        return new UnitMeasure(finalUnits, inMinutes(finalUnits, throughput));
    }

    private static Integer inMinutes(final Integer quantity, final Integer throughput) {
        if (quantity == null || throughput == null || throughput.equals(0)) {
            return null;
        }

        final double minutes = MINUTES_IN_HOUR * quantity / throughput;
        return (int) Math.ceil(minutes);
    }
}
