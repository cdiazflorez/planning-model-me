package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata;

import lombok.Builder;
import lombok.Getter;

import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorDataType.DEVIATION;

@Getter
@Builder
public class DeviationData extends MonitorData {

    public DeviationData() {
        this.type = DEVIATION.getType();
    }

}
