package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata;

import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorDataType.DEVIATION;

public class DeviationData extends MonitorData {

    public DeviationData() {
        this.type = DEVIATION.getType();
    }

}
