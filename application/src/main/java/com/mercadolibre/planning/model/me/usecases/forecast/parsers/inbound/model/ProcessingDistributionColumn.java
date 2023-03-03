package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.BACKLOG_UPPER_LIMIT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Process.CHECK_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Process.PUT_AWAY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Process.RECEIVING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Process.STAGE_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion.mapping;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.CellValue;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

@Getter
public enum ProcessingDistributionColumn {
    RECEIVING_TARGET(mapping(2, 2), THROUGHPUT, RECEIVING, UNITS_PER_HOUR, RepsRow::getReceivingWorkload),
    CHECK_IN_TARGET(mapping(3, 3), THROUGHPUT, CHECK_IN, UNITS_PER_HOUR, RepsRow::getCheckInWorkload),
    PUT_AWAY_TARGET(mapping(4, 4), THROUGHPUT, PUT_AWAY, UNITS_PER_HOUR, RepsRow::getPutAwayWorkload),
    STAGE_IN_TARGET(mapping(5, 5), PERFORMED_PROCESSING, STAGE_IN, UNITS_PER_HOUR, RepsRow::getStageInWorkload),
    ACTIVE_RECEIVING(mapping(-1, 7), EFFECTIVE_WORKERS, RECEIVING, WORKERS, RepsRow::getActiveRepsReceiving),
    ACTIVE_RECEIVING_NS(mapping(7, 8), EFFECTIVE_WORKERS_NS, RECEIVING, WORKERS, RepsRow::getActiveNsRepsReceiving),
    ACTIVE_CHECK_IN(mapping(8, 9), EFFECTIVE_WORKERS, CHECK_IN, WORKERS, RepsRow::getActiveRepsCheckIn),
    ACTIVE_CHECK_IN_NS(mapping(9, 10), EFFECTIVE_WORKERS_NS, CHECK_IN, WORKERS, RepsRow::getActiveNsRepsCheckIn),
    ACTIVE_PUT_AWAY(mapping(10, 11), EFFECTIVE_WORKERS, PUT_AWAY, WORKERS, RepsRow::getActiveRepsPutAway),
    ACTIVE_PUT_AWAY_NS(mapping(11, 12), EFFECTIVE_WORKERS_NS, PUT_AWAY, WORKERS, RepsRow::getActiveNsRepsPutAway),
    PRESENT_RECEIVING(mapping(-1, 13), ProcessingType.ACTIVE_WORKERS, RECEIVING, WORKERS, RepsRow::getPresentRepsReceiving),
    PRESENT_RECEIVING_NS(mapping(12, 14), ACTIVE_WORKERS_NS, RECEIVING, WORKERS, RepsRow::getPresentNsRepsReceiving),
    PRESENT_CHECK_IN(mapping(13, 15), ProcessingType.ACTIVE_WORKERS, CHECK_IN, WORKERS, RepsRow::getPresentRepsCheckIn),
    PRESENT_CHECK_IN_NS(mapping(14, 16), ACTIVE_WORKERS_NS, CHECK_IN, WORKERS, RepsRow::getPresentNsRepsCheckIn),
    PRESENT_PUT_AWAY(mapping(15, 17), ProcessingType.ACTIVE_WORKERS, PUT_AWAY, WORKERS, RepsRow::getPresentRepsPutAway),
    PRESENT_PUT_AWAY_NS(mapping(16, 18), ACTIVE_WORKERS_NS, PUT_AWAY, WORKERS, RepsRow::getPresentNsRepsPutAway),
    BACKLOG_LOWER_LIMIT_CHECK_IN(mapping(20, 22), BACKLOG_LOWER_LIMIT, CHECK_IN, MINUTES, RepsRow::getBacklogLowerLimitCheckin),
    BACKLOG_UPPER_LIMIT_CHECK_IN(mapping(21, 23), BACKLOG_UPPER_LIMIT, CHECK_IN, MINUTES, RepsRow::getBacklogUpperLimitCheckin),
    BACKLOG_LOWER_LIMIT_PUT_AWAY(mapping(22, 24), BACKLOG_LOWER_LIMIT, PUT_AWAY, MINUTES, RepsRow::getBacklogLowerLimitPutAway),
    BACKLOG_UPPER_LIMIT_PUT_AWAY(mapping(23, 25), BACKLOG_UPPER_LIMIT, PUT_AWAY, MINUTES, RepsRow::getBacklogUpperLimitPutAway);

    private final Map<SheetVersion, Integer> columnIdByVersion;
    private final ProcessingType type;
    private final Process process;
    private final MetricUnit unit;
    private final Function<RepsRow, Integer> mapper;

    ProcessingDistributionColumn(final Map<SheetVersion, Integer> columnIdByVersion,
                                 final ProcessingType type,
                                 final Process process,
                                 final MetricUnit unit,
                                 final IntegerMapper mapper) {
        this.columnIdByVersion = columnIdByVersion;
        this.type = type;
        this.process = process;
        this.unit = unit;
        this.mapper = row -> mapper.apply(row).getValue();
    }

    ProcessingDistributionColumn(final Map<SheetVersion, Integer> columnIdByVersion,
                                 final ProcessingType type,
                                 final Process process,
                                 final MetricUnit unit,
                                 final DoubleMapper mapper) {
        this.columnIdByVersion = columnIdByVersion;
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
