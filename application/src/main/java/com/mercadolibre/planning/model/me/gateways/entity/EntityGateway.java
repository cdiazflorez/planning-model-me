package com.mercadolibre.planning.model.me.gateways.entity;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.dtos.GetShareDistributionInput;

import java.util.List;

public interface EntityGateway {

  SaveUnitsResponse saveShareDistribution(final List<ShareDistribution> shareDistributionList, final Workflow workflow);

  List<GetUnitsResponse> getShareDistribution(final GetShareDistributionInput getShareDistributionInput, final Workflow workflow);
}
