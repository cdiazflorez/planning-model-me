package com.mercadolibre.planning.model.me.clients.rest.planningmodel.request;


import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantity;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class DeferralProjectionStatusRequest {

    Instant dateFrom;

    Instant dateTo;

    List<ProcessName> processName;

    List<BacklogQuantity> backlog;

    String warehouseId;

    String timeZone;

    boolean applyDeviation;

    List<Simulation> simulations;
}
