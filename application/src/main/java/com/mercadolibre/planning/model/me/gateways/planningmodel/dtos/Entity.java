package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;
import java.time.ZonedDateTime;

@Data
@SuperBuilder
public class Entity {

    private ZonedDateTime date;

    private Workflow workflow;

    private ProcessName processName;

    private int value;

    private Source source;

    private MetricUnit metricUnit;

    public LocalTime getTime() {
        return date.toLocalTime();
    }

}
