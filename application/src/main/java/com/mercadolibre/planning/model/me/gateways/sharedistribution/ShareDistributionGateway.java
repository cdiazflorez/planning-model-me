package com.mercadolibre.planning.model.me.gateways.sharedistribution;

import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionResponse;

import java.util.List;

public interface ShareDistributionGateway {

    List<DistributionResponse> getMetrics(String wareHouseId);

}
