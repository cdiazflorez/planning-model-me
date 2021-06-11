package com.mercadolibre.planning.model.me.gateways.staffing;

import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.GetStaffingRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;

public interface StaffingGateway {

    StaffingResponse getStaffing(final GetStaffingRequest request);
}
