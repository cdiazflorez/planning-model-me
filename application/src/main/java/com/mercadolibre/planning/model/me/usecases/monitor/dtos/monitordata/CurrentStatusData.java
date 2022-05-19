package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import java.util.TreeSet;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CurrentStatusData extends MonitorData {

    private final TreeSet<Process> processes;

    public CurrentStatusData(TreeSet<Process> processes) {
        super(MonitorDataType.CURRENT_STATUS.getType());
        this.processes = processes;
    }
}
