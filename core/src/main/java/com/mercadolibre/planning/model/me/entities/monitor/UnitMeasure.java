package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Value;

/**
 * The y coordinate of a point in the backlog over time graph, expressed both in units and in
 * minutes required to process them.
 */
@Value
public class UnitMeasure {
    private static final double MINUTES_IN_HOUR = 60.0;

    private Integer units;
    private Integer minutes;

    public static UnitMeasure emptyMeasure() {
        return new UnitMeasure(null, null);
    }

    public static UnitMeasure fromUnits(Integer units, Integer throughput) {
        final Integer finalUnits = units >= 0 ? units : 0;
        return new UnitMeasure(finalUnits, inMinutes(finalUnits, throughput));
    }

    public static UnitMeasure fromMinutes(Integer minutes, Integer throughput) {
        return new UnitMeasure(inUnits(minutes, throughput), minutes);
    }

    private static Integer inMinutes(final Integer quantity, final Integer throughput) {
        if (quantity == null || throughput == null || throughput.equals(0)) {
            return null;
        }

        final double minutes = MINUTES_IN_HOUR * quantity / throughput;
        return (int) Math.ceil(minutes);
    }

    private static Integer inUnits(final Integer minutes, final Integer throughput) {
        if (minutes == null || throughput == null || throughput.equals(0)) {
            return null;
        }

        final double units = throughput * minutes / MINUTES_IN_HOUR;
        return (int) Math.ceil(units);
    }
}
