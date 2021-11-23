package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagVarPhoto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CurrentStatusMetricInputs {

    final ProcessBacklog processBacklog;
    final GetCurrentStatusInput input;
    final List<UnitsResume> processedUnitsLastHour;
    final List<MagVarPhoto> productivityHeadCounts;
}
