package com.mercadolibre.planning.model.me.gateways.entity;

import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.dtos.GetShareDistributionInput;
import java.util.List;

/** Connection gateway with planning-model-api to save and consult share distribution. */
public interface EntityGateway {

  SaveUnitsResponse saveShareDistribution(List<ShareDistribution> shareDistributionList, Workflow workflow);

  List<GetUnitsResponse> getShareDistribution(GetShareDistributionInput getShareDistributionInput, Workflow workflow);
}
