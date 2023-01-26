package com.mercadolibre.planning.model.me.gateways.inboundreports;

import com.mercadolibre.planning.model.me.gateways.inboundreports.dto.InboundResponse;
import java.time.Instant;

/**
 * This gateway gets the units received by the warehouse in a range of dates and filters by a type of shipment.
 */
public interface InboundReportsApiGateway {

  InboundResponse getUnitsReceived(String warehouseId, Instant lastArrivalDateFrom, Instant lastArrivalDateTo, String shipmentType);

}
