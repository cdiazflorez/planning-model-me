package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

@AllArgsConstructor
@Getter
public enum ProcessInfo {
    OUTBOUND_PLANNING("pending", "Ready to Wave", "Outbound Planning", 0,
            asList(BACKLOG, THROUGHPUT_PER_HOUR)),
    PICKING("to_pick", "Ready to Pick", "Picking", 1,
            asList(BACKLOG, THROUGHPUT_PER_HOUR, PRODUCTIVITY)),
    PACKING("to_pack", "Ready to Pack", "Packing", 2,
            asList(BACKLOG, THROUGHPUT_PER_HOUR, PRODUCTIVITY)),
    WALL_IN("to_sort,sorted,to_group", "Ready to Group", "Wall In", 3,
            asList(BACKLOG));
    private final String status;
    private final String subtitle;
    private final String title;
    private final Integer index;
    private final List<MetricType> metricTypes;

    public static ProcessInfo fromTitle(String title) {
        return stream(ProcessInfo.values())
                .filter(pInfo -> pInfo.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);
    }
}
