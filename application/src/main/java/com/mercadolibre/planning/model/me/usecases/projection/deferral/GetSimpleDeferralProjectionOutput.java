package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;


@Value
@AllArgsConstructor
public class GetSimpleDeferralProjectionOutput {

    public List<ProjectionResult> projections;

    public LogisticCenterConfiguration configuration;
}
