package com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PackingRatio;
import java.time.Instant;
import java.util.Map;

/**
 * Interface used for connect with planning-model-api.
 *
 * <P>Consume new getPackingWallRatios endpoint</P>
 */
public interface PackingWallRatiosGateway {

  Map<Instant, PackingRatio> getPackingWallRatios(String logisticCenterId, Instant dateFrom, Instant dateTo);

}
