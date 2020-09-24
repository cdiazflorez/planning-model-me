package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;
import java.time.ZonedDateTime;

@Value
@Builder
public class Entity {

    private ZonedDateTime date;

    private Workflow workflow;

    private ProcessName processName;

    private int value;

    private Source source;

    private MetricUnit metricUnit;

    public int getHour() {
        return date.getHour();
    }

    public LocalTime getTime() {
        return date.toLocalTime();
    }

}
