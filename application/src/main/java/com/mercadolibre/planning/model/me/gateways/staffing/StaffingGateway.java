package com.mercadolibre.planning.model.me.gateways.staffing;

import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.MetricRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.MetricResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;

public interface StaffingGateway {

    StaffingResponse getStaffing(final String logisticCenter);

    MetricResponse getMetricsByName(String logisticCenter, String metricName, MetricRequest metricRequest);
}
