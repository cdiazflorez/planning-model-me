package com.mercadolibre.planning.model.me.entities.monitor;

import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class WorkflowBacklogDetail {
    private String workflow;
    private Instant currentDatetime;
    private List<ProcessDetail> processes;
}
