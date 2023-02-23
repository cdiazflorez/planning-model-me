package com.mercadolibre.planning.model.me.services.backlog;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PackingRatio;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.PackingWallRatiosGateway;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.Map;
import javax.inject.Named;
import lombok.AllArgsConstructor;

/**
 * RatioService provides the methods to retrieve backlog ratio metrics.
 */
@Named
@AllArgsConstructor
public class RatioService {

  private PackingWallRatiosGateway packingWallRatiosGateway;

  /**
   * This logic was migrated to planning-model-api in two parts.
   * 1. The job that preload the ratios
   * 2. The endpoint that calculates the final ratio with the ratios
   *
   * @param logisticCenterId warehouse id.
   * @param dateFrom         starting date for which the ratio is desired.
   * @param dateTo           ending date for which the ratio is desired.
   * @return A list of ratio per day for both packing and consolidation flow bifurcation.
   */
  @Trace
  public Map<Instant, PackingRatio> getPackingRatio(
      final String logisticCenterId,
      final Instant dateFrom,
      final Instant dateTo) {

    return packingWallRatiosGateway.getPackingWallRatios(logisticCenterId, dateFrom, dateTo);

  }
}
