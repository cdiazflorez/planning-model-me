package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.IMMEDIATE_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;

@AllArgsConstructor
@Getter
public enum ProcessOutbound {

    OUTBOUND_PLANNING(
            "pending",
            "Ready to Wave",
            "Outbound Planning",
            0,
            asList(TOTAL_BACKLOG, IMMEDIATE_BACKLOG, THROUGHPUT_PER_HOUR)),
    PICKING(
            "to_pick",
            "Ready to Pick",
            "Picking",
            1,
            singletonList(TOTAL_BACKLOG)),
    WALL_IN(
            "sorted,to_group",
            "Ready to Group",
            "Wall",
            2,
            singletonList(TOTAL_BACKLOG)),
    PACKING(
            "to_pack",
            "Ready to Pack",
            "Packing normal",
            3,
            singletonList(TOTAL_BACKLOG)),
    PACKING_WALL(
            "to_pack",
            "Ready to Pack",
            "Packing de wall",
            4,
            singletonList(TOTAL_BACKLOG));

    private final String status;
    private final String subtitle;
    private final String title;
    private final Integer index;
    private final List<MetricType> metricTypes;

    public static ProcessOutbound fromTitle(String title) {
        return stream(ProcessOutbound.values())
                .filter(pInfo -> pInfo.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);
    }
}
