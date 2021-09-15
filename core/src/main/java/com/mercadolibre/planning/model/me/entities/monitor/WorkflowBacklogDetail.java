package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class WorkflowBacklogDetail {
    private String workflow;
    private ZonedDateTime currentDatetime;
    private List<ProcessDetail> processes;
}
