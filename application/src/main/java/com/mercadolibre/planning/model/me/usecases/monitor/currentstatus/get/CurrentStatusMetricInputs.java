package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurrentStatusMetricInputs {

    final ProcessBacklog processBacklog;
    final GetCurrentStatusInput input;
    final List<UnitsResume> processedUnitsLastHour;
    final List<MagnitudePhoto> productivityHeadCounts;
}
