package com.mercadolibre.planning.model.me.gateways.backlog.dto;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class BacklogCurrentRequest {
    private Instant requestDate;
    private String warehouseId;
    private List<String> workflows;
    private List<String> processes;
    private List<String> steps;
    private List<String> groupingFields;
    private Instant dateInFrom;
    private Instant dateInTo;
    private Instant slaFrom;
    private Instant slaTo;

    public BacklogCurrentRequest(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public BacklogCurrentRequest withSlaRange(Instant from, Instant to) {
        this.slaFrom = from;
        this.slaTo = to;
        return this;
    }

    public BacklogCurrentRequest withDateInRange(Instant from, Instant to) {
        this.dateInFrom = from;
        this.dateInTo = to;
        return this;
    }

    public BacklogCurrentRequest withRequestDate(Instant requestDate) {
        this.requestDate = requestDate;
        return this;
    }

    public BacklogCurrentRequest withWorkflows(List<String> workflows) {
        this.workflows = workflows;
        return this;
    }

    public BacklogCurrentRequest withProcesses(List<String> processes) {
        this.processes = processes;
        return this;
    }

    public BacklogCurrentRequest withSteps(List<String> steps) {
        this.steps = steps;
        return this;
    }

    public BacklogCurrentRequest withGroupingFields(List<String> groupingFields) {
        this.groupingFields = groupingFields;
        return this;
    }
}
