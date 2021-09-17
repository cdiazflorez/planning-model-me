package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.BacklogsByDate;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.ProcessDetailBuilderInput;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class ProcessDetailBuilder {

    private ProcessDetailBuilder() { }

    static Integer inMinutes(Integer quantity, Integer throughput) {
        if (quantity == null || throughput == null || throughput.equals(0)) {
            return null;
        }
        return (int) Math.ceil((double) quantity / throughput);
    }

    static ProcessDetail build(ProcessDetailBuilderInput input) {
        final List<BacklogsByDate> backlog = input.getBacklogs()
                .stream()
                .map(stats -> toBacklogByDate(stats, input.getCurrentDatetime()))
                .sorted(Comparator.comparing(BacklogsByDate::getDate))
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

    private static BacklogsByDate toBacklogByDate(BacklogStatsByDate description,
                                                  ZonedDateTime currentDatetime) {

        ZonedDateTime date = currentDatetime.equals(description.getDate())
                ? currentDatetime
                : description.getDate().truncatedTo(ChronoUnit.HOURS);

        Integer units = description.getUnits() >= 0 ? description.getUnits() : 0;

        return BacklogsByDate.builder()
                .date(date)
                .current(
                        new UnitMeasure(
                                units,
                                inMinutes(
                                        units,
                                        description.getThroughput()))
                )
                .historical(
                        new UnitMeasure(description.getHistorical(), null)
                )
                .build();
    }
}
