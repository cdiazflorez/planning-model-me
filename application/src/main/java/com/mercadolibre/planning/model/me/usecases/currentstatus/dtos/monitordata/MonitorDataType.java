package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MonitorDataType {
    DEVIATION("deviation"),
    CURRENT_STATUS("current_status");

    private final String type;

}
