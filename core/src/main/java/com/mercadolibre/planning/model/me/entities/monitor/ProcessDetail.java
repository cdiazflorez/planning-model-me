package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Value;

import java.util.List;

/** Backlog-related information of a process. */
@Value
public class ProcessDetail {
    /** The name of process in question. */
    private String process;
    /** The total backlog at the current time of the process in question. */
    private UnitMeasure total;
    /** The trajectory of backlog-related variables of the process in question.
     * Used to draw the backlog-over-time graph. */
    private List<VariablesPhoto> backlogs;
}
