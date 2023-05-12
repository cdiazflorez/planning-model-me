package com.mercadolibre.planning.model.me.gateways.staffing.dtos.request;

import java.time.ZonedDateTime;
import java.util.List;
import lombok.Value;

@Value
public class GetStaffingRequest {

    final ZonedDateTime synchronizationDateFrom;

    final ZonedDateTime synchronizationDateTo;

    final String logisticCenterId;

    final List<Aggregation> aggregations;

}
