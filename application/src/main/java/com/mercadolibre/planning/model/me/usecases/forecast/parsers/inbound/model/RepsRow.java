package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.CellValue;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class RepsRow {
    CellValue<ZonedDateTime> date;
    CellValue<Integer> receivingWorkload;
    CellValue<Integer> checkInWorkload;
    CellValue<Integer> putAwayWorkload;
    CellValue<Integer> stageInWorkload;
    CellValue<Integer> activeRepsReceiving;
    CellValue<Integer> activeNsRepsReceiving;
    CellValue<Integer> activeRepsCheckIn;
    CellValue<Integer> activeNsRepsCheckIn;
    CellValue<Integer> activeRepsPutAway;
    CellValue<Integer> activeNsRepsPutAway;
    CellValue<Integer> presentRepsReceiving;
    CellValue<Integer> presentNsRepsReceiving;
    CellValue<Integer> presentRepsCheckIn;
    CellValue<Integer> presentNsRepsCheckIn;
    CellValue<Integer> presentRepsPutAway;
    CellValue<Integer> presentNsRepsPutAway;
    CellValue<Double> backlogLowerLimitCheckin;
    CellValue<Double> backlogUpperLimitCheckin;
    CellValue<Double> backlogLowerLimitPutAway;
    CellValue<Double> backlogUpperLimitPutAway;
}
