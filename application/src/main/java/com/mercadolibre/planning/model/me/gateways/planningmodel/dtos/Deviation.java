package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.utils.CustomDateZoneDeserializer;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Deviation {

    Workflow workflow;
    String type;
    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    ZonedDateTime dateFrom;
    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    ZonedDateTime dateTo;
    double value;
    MetricUnit metricUnit;

}

