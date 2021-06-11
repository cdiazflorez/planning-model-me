package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.IMMEDIATE_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;

@AllArgsConstructor
@Getter
public enum ProcessInfo {

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
            asList(TOTAL_BACKLOG, THROUGHPUT_PER_HOUR, PRODUCTIVITY)),
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
            asList(TOTAL_BACKLOG, THROUGHPUT_PER_HOUR, PRODUCTIVITY)),
    PACKING_WALL(
            "to_pack",
            "Ready to Pack",
            "Packing de wall",
            4,
            asList(TOTAL_BACKLOG, THROUGHPUT_PER_HOUR, PRODUCTIVITY));

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
