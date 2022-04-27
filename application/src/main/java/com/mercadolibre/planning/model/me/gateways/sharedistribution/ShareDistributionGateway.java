package com.mercadolibre.planning.model.me.gateways.sharedistribution;

import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionElement;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

/** Gateway for consult history backlogs. */
public interface ShareDistributionGateway {

  List<DistributionElement> getMetrics(String wareHouseId, Instant startDate, Instant endDate);

}
