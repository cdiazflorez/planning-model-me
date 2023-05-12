package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.IMMEDIATE_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessInbound {

    INBOUND_PLANNING(
            "scheduled",
            "Ready to CheckIn",
            "Inbound Planning",
            0,
            asList(TOTAL_BACKLOG, IMMEDIATE_BACKLOG, THROUGHPUT_PER_HOUR)),
    CHECK_IN(
            "check_in",
            "Ready to CheckIn",
            "CheckIn",
            1,
            singletonList(TOTAL_BACKLOG)),
    PUT_AWAY(
            "put_away",
            "Ready to PutAway",
            "PutAway",
            2,
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
