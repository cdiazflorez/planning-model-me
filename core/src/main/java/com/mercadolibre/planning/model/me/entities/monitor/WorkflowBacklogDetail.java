package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
public class WorkflowBacklogDetail {
    private String workflow;
    private Instant currentDatetime;
    private List<ProcessDetail> processes;
}
