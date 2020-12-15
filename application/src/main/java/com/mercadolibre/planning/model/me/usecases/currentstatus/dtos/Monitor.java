package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorData;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
public class Monitor {

    private final String title;
    @JsonProperty("subtitle_1")
    private final String subtitle1;
    @JsonProperty("subtitle_2")
    private final String subtitle2;
    private final List<MonitorData> monitorData;

}
