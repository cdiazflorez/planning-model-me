package com.mercadolibre.planning.model.me.usecases.monitor.metric.productivity;

import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ProductivityInput {

    final GetMonitorInput monitorInput;
    final UnitsResume processedUnitLastHour;
    final ProcessInfo processInfo;

}
