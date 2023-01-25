package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.CellValue;
import lombok.Getter;

import java.util.function.Function;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.BACKLOG_UPPER_LIMIT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.WORKERS_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Process.CHECK_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Process.PUT_AWAY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Process.RECEIVING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Process.STAGE_IN;

@Getter
public enum ProcessingDistributionColumn {
    RECEIVING_TARGET(2, THROUGHPUT, RECEIVING, UNITS_PER_HOUR, RepsRow::getReceivingWorkload),
    CHECK_IN_TARGET(3, THROUGHPUT, CHECK_IN, UNITS_PER_HOUR, RepsRow::getCheckInWorkload),
    PUT_AWAY_TARGET(4, THROUGHPUT, PUT_AWAY, UNITS_PER_HOUR, RepsRow::getPutAwayWorkload),
    STAGE_IN_TARGET(5, PERFORMED_PROCESSING, STAGE_IN, UNITS_PER_HOUR, RepsRow::getStageInWorkload),
    ACTIVE_RECEIVING_NS(7, ACTIVE_WORKERS_NS, RECEIVING, WORKERS, RepsRow::getActiveNsRepsReceiving),
    ACTIVE_CHECK_IN(8, ACTIVE_WORKERS, CHECK_IN, WORKERS, RepsRow::getActiveRepsCheckIn),
    ACTIVE_CHECK_IN_NS(9, ACTIVE_WORKERS_NS, CHECK_IN, WORKERS, RepsRow::getActiveNsRepsCheckIn),
    ACTIVE_PUT_AWAY(10, ACTIVE_WORKERS, PUT_AWAY, WORKERS, RepsRow::getActiveRepsPutAway),
    ACTIVE_PUT_AWAY_NS(11, ACTIVE_WORKERS_NS, PUT_AWAY, WORKERS, RepsRow::getActiveNsRepsPutAway),
    PRESENT_RECEIVING_NS(12, WORKERS_NS, RECEIVING, WORKERS, RepsRow::getPresentNsRepsReceiving),
    PRESENT_CHECK_IN(13, ProcessingType.WORKERS, CHECK_IN, WORKERS, RepsRow::getPresentRepsCheckIn),
    PRESENT_CHECK_IN_NS(14, WORKERS_NS, CHECK_IN, WORKERS, RepsRow::getPresentNsRepsCheckIn),
    PRESENT_PUT_AWAY(15, ProcessingType.WORKERS, PUT_AWAY, WORKERS, RepsRow::getPresentRepsPutAway),
    PRESENT_PUT_AWAY_NS(16, WORKERS_NS, PUT_AWAY, WORKERS, RepsRow::getPresentNsRepsPutAway),
    BACKLOG_LOWER_LIMIT_CHECK_IN(20, BACKLOG_LOWER_LIMIT, CHECK_IN, MINUTES, RepsRow::getBacklogLowerLimitCheckin),
    BACKLOG_UPPER_LIMIT_CHECK_IN(21, BACKLOG_UPPER_LIMIT, CHECK_IN, MINUTES, RepsRow::getBacklogUpperLimitCheckin),
    BACKLOG_LOWER_LIMIT_PUT_AWAY(22, BACKLOG_LOWER_LIMIT, PUT_AWAY, MINUTES, RepsRow::getBacklogLowerLimitPutAway),
    BACKLOG_UPPER_LIMIT_PUT_AWAY(23, BACKLOG_UPPER_LIMIT, PUT_AWAY, MINUTES, RepsRow::getBacklogUpperLimitPutAway);

    private Integer columnId;
    private ProcessingType type;
    private Process process;
    private MetricUnit unit;
    private Function<RepsRow, Integer> mapper;

    ProcessingDistributionColumn(final Integer columnId,
                                 final ProcessingType type,
                                 final Process process,
                                 final MetricUnit unit,
                                 final IntegerMapper mapper) {
        this.columnId = columnId;
        this.type = type;
        this.process = process;
        this.unit = unit;
        this.mapper = row -> mapper.apply(row).getValue();
    }

    ProcessingDistributionColumn(final Integer columnId,
                                 final ProcessingType type,
                                 final Process process,
                                 final MetricUnit unit,
                                 final DoubleMapper mapper) {
        this.columnId = columnId;
        this.type = type;
        this.process = process;
        this.unit = unit;
        this.mapper = row -> (int)(mapper.apply(row).getValue() < 0
                ? 0 : mapper.apply(row).getValue() * 60.0);
    }

    @FunctionalInterface
    interface IntegerMapper {
        CellValue<Integer> apply(final RepsRow repsRow);
    }

    @FunctionalInterface
    interface DoubleMapper {
        CellValue<Double> apply(final RepsRow repsRow);
    }
}
