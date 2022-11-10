package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
public class PlanningDistribution {
    private ZonedDateTime dateIn;
    private ZonedDateTime dateOut;
    private String carrierId;
    private String serviceId;
    private String canalization;
    private ProcessPath processPath;
    private double quantity;
    private String quantityMetricUnit;
    private List<String> metadata;
}
