package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.BacklogsByDate;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.ProcessDetailBuilderInput;

import javax.inject.Named;

import java.util.List;
import java.util.stream.Collectors;

@Named
class ProcessDetailBuilder {
    ProcessDetail execute(ProcessDetailBuilderInput input) {
        final List<BacklogsByDate> backlog = input.getBacklogs()
                .stream()
                .map(this::toBacklogByDate)
                .collect(Collectors.toList());

        final UnitMeasure totals = backlog.stream()
                .filter(b -> b.getDate().equals(input.getCurrentDatetime()))
                .findFirst()
                .map(BacklogsByDate::getCurrent)
                .map(measure -> new UnitMeasure(
                        measure.getUnits(),
                        measure.getMinutes() == null ? Integer.valueOf(0) : measure.getMinutes()))
                .orElse(new UnitMeasure(0, 0));

        return new ProcessDetail(input.getProcess().getName(), totals, backlog);
    }

    private BacklogsByDate toBacklogByDate(BacklogStatsByDate description) {
        return BacklogsByDate.builder()
                .date(description.getDate())
                .current(
                        new UnitMeasure(
                                description.getUnits(),
                                inMinutes(
                                        description.getUnits(),
                                        description.getThroughput()))
                )
                .historical(
                        new UnitMeasure(description.getHistorical(), null)
                )
                .build();
    }

    private Integer inMinutes(Integer quantity, Integer throughput) {
        if (quantity == null || throughput == null || throughput.equals(0)) {
            return null;
        }
        return quantity / throughput;
    }
}
