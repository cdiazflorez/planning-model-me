package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.CellValue;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class RepsRow {
    private CellValue<ZonedDateTime> date;
    private CellValue<Integer> receivingWorkload;
    private CellValue<Integer> checkInWorkload;
    private CellValue<Integer> putAwayWorkload;
    private CellValue<Integer> stageInWorkload;
    private CellValue<Integer> activeNsRepsReceiving;
    private CellValue<Integer> activeRepsCheckIn;
    private CellValue<Integer> activeNsRepsCheckIn;
    private CellValue<Integer> activeRepsPutAway;
    private CellValue<Integer> activeNsRepsPutAway;
    private CellValue<Integer> presentNsRepsReceiving;
    private CellValue<Integer> presentRepsCheckIn;
    private CellValue<Integer> presentNsRepsCheckIn;
    private CellValue<Integer> presentRepsPutAway;
    private CellValue<Integer> presentNsRepsPutAway;
    private CellValue<Double> backlogLowerLimitCheckin;
    private CellValue<Double> backlogUpperLimitCheckin;
    private CellValue<Double> backlogLowerLimitPutAway;
    private CellValue<Double> backlogUpperLimitPutAway;
}
