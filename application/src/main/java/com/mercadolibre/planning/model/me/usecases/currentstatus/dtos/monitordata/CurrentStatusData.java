package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata;

import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Process;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorDataType.CURRENT_STATUS;

@Builder
@Getter
public class CurrentStatusData extends MonitorData {

    private final List<Process> processes;

    public CurrentStatusData(List<Process> processes) {
        super(CURRENT_STATUS.getType());
        this.processes = processes;
    }
}
