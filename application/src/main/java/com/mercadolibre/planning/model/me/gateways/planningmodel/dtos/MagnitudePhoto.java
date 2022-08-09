package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

/** A tagged photo of a scalar magnitude variable. */
@Data
@SuperBuilder
public class MagnitudePhoto {

    /** The date when the photo was taken. */
    private ZonedDateTime date;

    /** The workflow to which the variable belongs. */
    private Workflow workflow;

    /** The name of the process to which the variable corresponds. */
    private ProcessName processName;

    /** The value of the variable when the photo was taken. */
    private int value;

    /** The instance of the process (forecast or simulation) to which the variable corresponds. */
    private Source source;

    /** The unit in which the magnitude is expressed. */
    private MetricUnit metricUnit;
}
