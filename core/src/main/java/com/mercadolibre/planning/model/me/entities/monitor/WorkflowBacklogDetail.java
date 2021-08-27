package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Value;

import java.util.List;

@Value
public class WorkflowBacklogDetail {
    private String workflow;
    private List<ProcessDetail> processes;
}
