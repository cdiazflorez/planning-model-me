package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.utils.CustomDateZoneDeserializer;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class GetDeviationResponse {

    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime dateFrom;
    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime dateTo;
    private double value;
    private MetricUnit metricUnit;

}
