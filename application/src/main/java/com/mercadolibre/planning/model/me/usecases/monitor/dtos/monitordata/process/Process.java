package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Builder
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class Process implements Comparable<Process> {
    private final String title;
    private final List<Metric> metrics;

    @Override
    public int compareTo(Process other) {
        final Integer thisIndex = ProcessOutbound.fromTitle(this.title).getIndex();
        final Integer otherIndex = ProcessOutbound.fromTitle(other.getTitle()).getIndex();
        return Integer.compare(thisIndex, otherIndex);
    }
}
