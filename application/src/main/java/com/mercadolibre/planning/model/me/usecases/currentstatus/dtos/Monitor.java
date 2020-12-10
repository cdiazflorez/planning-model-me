package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorData;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Monitor {

    private String title;
    @JsonProperty("subtitle_1")
    private String subtitle1;
    @JsonProperty("subtitle_2")
    private String subtitle2;
    private List<MonitorData> monitorData;

}
